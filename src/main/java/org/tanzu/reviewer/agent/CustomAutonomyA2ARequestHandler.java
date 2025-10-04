package org.tanzu.reviewer.agent;

import com.embabel.agent.a2a.server.A2ARequestHandler;
import com.embabel.agent.a2a.server.support.A2AStreamingHandler;
import com.embabel.agent.api.common.autonomy.AgentProcessExecution;
import com.embabel.agent.api.common.autonomy.Autonomy;
import com.embabel.agent.core.ProcessOptions;
import io.a2a.spec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Custom A2A request handler that emits intermediate Story and ReviewedStory outputs
 * as A2A messages during execution.
 *
 * Handles both streaming and non-streaming requests.
 */
@Service
@Profile("a2a")
@Primary
public class CustomAutonomyA2ARequestHandler implements A2ARequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAutonomyA2ARequestHandler.class);

    private final Autonomy autonomy;
    private final A2AStreamingHandler streamingHandler;
    private final A2AOutputEmitter a2aOutputEmitter;

    public CustomAutonomyA2ARequestHandler(
            Autonomy autonomy,
            A2AStreamingHandler streamingHandler,
            A2AOutputEmitter a2aOutputEmitter
    ) {
        this.autonomy = autonomy;
        this.streamingHandler = streamingHandler;
        this.a2aOutputEmitter = a2aOutputEmitter;
    }

    @Override
    public JSONRPCResponse<?> handleJsonRpc(NonStreamingJSONRPCRequest<?> request) {
        if (request instanceof SendMessageRequest messageRequest) {
            return handleNonStreamingMessage(messageRequest);
        }
        throw new UnsupportedOperationException("Method " + request.getMethod() + " is not supported");
    }

    @Override
    public SseEmitter handleJsonRpcStream(StreamingJSONRPCRequest<?> request) {
        if (request instanceof SendStreamingMessageRequest streamRequest) {
            return handleStreamingMessage(streamRequest);
        }
        throw new UnsupportedOperationException("Method " + request.getMethod() + " is not supported for streaming");
    }

    private SendMessageResponse handleNonStreamingMessage(SendMessageRequest request) {
        MessageSendParams params = request.getParams();

        try {
            // Start collecting artifacts for this non-streaming request
            a2aOutputEmitter.startCollecting();

            // Extract intent from message
            String intent = extractIntent(params.message());

            logger.info("Handling message send request with intent: '{}'", intent);

            // Execute with custom ProcessOptions that include our listener
            AgentProcessExecution result = autonomy.chooseAndRunAgent(
                    intent,
                    new ProcessOptions.Builder()
                            .listener(a2aOutputEmitter)
                            .build()
            );

            logger.debug("Task execution result: {}", result);

            // Collect all artifacts (final result + intermediate artifacts)
            List<Artifact> allArtifacts = new ArrayList<>(a2aOutputEmitter.getCollectedArtifacts());

            // Add the final result artifact
            allArtifacts.add(createResultArtifact(
                    result,
                    params.configuration() != null ?
                            params.configuration().acceptedOutputModes() : null
            ));

            // Build the response with all artifacts
            Task taskResult = new Task.Builder()
                    .id(params.message().getTaskId())
                    .contextId(ensureContextId(params.message().getContextId()))
                    .status(createCompletedTaskStatus(params))
                    .history(List.of(params.message()))
                    .artifacts(allArtifacts)
                    .metadata(null)
                    .build();

            logger.info("Handled message send request with {} artifacts", allArtifacts.size());

            // Return response with Task as EventKind
            return new SendMessageResponse(request.getId(), taskResult);

        } catch (Exception e) {
            logger.error("Error handling non-streaming message request", e);

            Task errorTask = new Task.Builder()
                    .id(params.message().getTaskId())
                    .contextId(ensureContextId(params.message().getContextId()))
                    .status(createFailedTaskStatus(params, e))
                    .history(List.of(params.message()))
                    .artifacts(List.of())
                    .metadata(null)
                    .build();

            return new SendMessageResponse(request.getId(), errorTask);
        } finally {
            // Clean up thread-local storage
            a2aOutputEmitter.clear();
        }
    }

    private SseEmitter handleStreamingMessage(SendStreamingMessageRequest request) {
        MessageSendParams params = request.getParams();
        String streamId = request.getId() != null ?
                request.getId().toString() : UUID.randomUUID().toString();

        SseEmitter emitter = streamingHandler.createStream(streamId);

        Thread.startVirtualThread(() -> {
            try {
                // Set the stream ID in the emitter for this thread
                a2aOutputEmitter.setStreamId(streamId);

                // Send initial status
                streamingHandler.sendStreamEvent(
                        streamId,
                        new TaskStatusUpdateEvent.Builder()
                                .taskId(params.message().getTaskId())
                                .contextId(params.message().getContextId())
                                .status(createWorkingTaskStatus(params, "Task started..."))
                                .build()
                );

                // Extract intent from message
                String intent = extractIntent(params.message());

                logger.info("Executing streaming task with intent: '{}'", intent);

                // Execute with custom ProcessOptions that include our listener
                AgentProcessExecution result = autonomy.chooseAndRunAgent(
                        intent,
                        new ProcessOptions.Builder()
                                .listener(a2aOutputEmitter)
                                .build()
                );

                logger.debug("Task execution result: {}", result);

                // Send intermediate status update
                streamingHandler.sendStreamEvent(
                        streamId,
                        new TaskStatusUpdateEvent.Builder()
                                .taskId(params.message().getTaskId())
                                .contextId(ensureContextId(params.message().getContextId()))
                                .status(createWorkingTaskStatus(params, "Processing task..."))
                                .build()
                );

                // Send final result
                Task taskResult = new Task.Builder()
                        .id(params.message().getTaskId())
                        .contextId("ctx_" + UUID.randomUUID())
                        .status(createCompletedTaskStatus(params))
                        .history(List.of(params.message()))
                        .artifacts(List.of(
                                createResultArtifact(result,
                                        params.configuration() != null ?
                                                params.configuration().acceptedOutputModes() : null)
                        ))
                        .metadata(null)
                        .build();

                streamingHandler.sendStreamEvent(streamId, taskResult);

            } catch (Exception e) {
                logger.error("Streaming error", e);
                try {
                    streamingHandler.sendStreamEvent(
                            streamId,
                            new TaskStatusUpdateEvent.Builder()
                                    .taskId(params.message().getTaskId())
                                    .contextId(ensureContextId(params.message().getContextId()))
                                    .status(createFailedTaskStatus(params, e))
                                    .build()
                    );
                } catch (Exception sendError) {
                    logger.error("Error sending error event", sendError);
                }
            } finally {
                // Clean up thread-local storage
                a2aOutputEmitter.clear();
                streamingHandler.closeStream(streamId);
            }
        });

        return emitter;
    }

    private String extractIntent(Message message) {
        return message.getParts().stream()
                .filter(part -> part instanceof TextPart)
                .map(part -> ((TextPart) part).getText())
                .findFirst()
                .orElse("Task " + message.getTaskId());
    }

    private TaskStatus createWorkingTaskStatus(MessageSendParams params, String textPart) {
        return new TaskStatus(
                TaskState.WORKING,
                new Message.Builder()
                        .messageId(UUID.randomUUID().toString())
                        .role(Message.Role.AGENT)
                        .parts(List.of(new TextPart(textPart)))
                        .contextId(params.message().getContextId())
                        .taskId(params.message().getTaskId())
                        .build(),
                LocalDateTime.now()
        );
    }

    private TaskStatus createCompletedTaskStatus(MessageSendParams params) {
        return new TaskStatus(
                TaskState.COMPLETED,
                new Message.Builder()
                        .messageId(UUID.randomUUID().toString())
                        .role(Message.Role.AGENT)
                        .parts(List.of(new TextPart("Task completed successfully")))
                        .contextId(params.message().getContextId())
                        .taskId(params.message().getTaskId())
                        .build(),
                LocalDateTime.now()
        );
    }

    private TaskStatus createFailedTaskStatus(MessageSendParams params, Exception error) {
        return new TaskStatus(
                TaskState.FAILED,
                new Message.Builder()
                        .messageId(UUID.randomUUID().toString())
                        .role(Message.Role.AGENT)
                        .parts(List.of(new TextPart("Task failed: " + error.getMessage())))
                        .contextId(params.message().getContextId())
                        .taskId(params.message().getTaskId())
                        .build(),
                LocalDateTime.now()
        );
    }

    private Artifact createResultArtifact(AgentProcessExecution result, List<String> acceptedOutputModes) {
        Map<String, Object> data = Map.of(
                "result", result.getOutput().toString(),
                "type", "final_result"
        );

        return new Artifact.Builder()
                .artifactId(UUID.randomUUID().toString())
                .parts(List.of(new DataPart(data)))
                .build();
    }

    private String ensureContextId(String contextId) {
        return contextId != null ? contextId : "ctx_" + UUID.randomUUID();
    }
}