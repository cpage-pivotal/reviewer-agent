package org.tanzu.reviewer.agent;

import com.embabel.agent.a2a.server.A2ARequestHandler;
import com.embabel.agent.a2a.server.support.A2AStreamingHandler;
import com.embabel.agent.api.common.autonomy.AgentProcessExecution;
import com.embabel.agent.api.common.autonomy.Autonomy;
import com.embabel.agent.core.ProcessOptions;
import io.a2a.spec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Custom A2A request handler that emits intermediate Story and ReviewedStory outputs
 * as A2A messages during streaming execution.
 */
@Service
@Profile("a2a")
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
        // Implement non-streaming handler if needed
        throw new UnsupportedOperationException("Non-streaming not implemented in this example");
    }

    @Override
    public SseEmitter handleJsonRpcStream(StreamingJSONRPCRequest<?> request) {
        if (request instanceof SendStreamingMessageRequest streamRequest) {
            return handleMessageStream(streamRequest);
        }
        throw new UnsupportedOperationException("Method " + request.getMethod() + " is not supported for streaming");
    }

    private SseEmitter handleMessageStream(SendStreamingMessageRequest request) {
        MessageSendParams params = request.getParams();
        String streamId = request.getId() != null ? request.getId().toString() : UUID.randomUUID().toString();
        SseEmitter emitter = streamingHandler.createStream(streamId);

        Thread.startVirtualThread(() -> {
            try {
                // Set the stream ID in the emitter for this thread
                a2aOutputEmitter.setStreamId(streamId);

                // Send initial status
                streamingHandler.sendStreamEvent(
                        streamId,
                        new TaskStatusUpdateEvent.Builder()
                                .taskId(params.getMessage().getTaskId())
                                .contextId(params.getMessage().getContextId())
                                .status(createWorkingTaskStatus(params, "Task started..."))
                                .build()
                );

                // Extract intent from message
                String intent = params.getMessage().getParts().stream()
                        .filter(part -> part instanceof TextPart)
                        .map(part -> ((TextPart) part).getText())
                        .findFirst()
                        .orElse("Task " + params.getMessage().getTaskId());

                logger.info("Executing task with intent: '{}'", intent);

                // Execute with custom ProcessOptions that include our listener
                AgentProcessExecution result = autonomy.chooseAndRunAgent(
                        intent,
                        new ProcessOptions.Builder()
                                .withListeners(List.of(a2aOutputEmitter))
                                .build()
                );

                logger.debug("Task execution result: {}", result);

                // Send intermediate status update
                streamingHandler.sendStreamEvent(
                        streamId,
                        new TaskStatusUpdateEvent.Builder()
                                .taskId(params.getMessage().getTaskId())
                                .contextId(ensureContextId(params.getMessage().getContextId()))
                                .status(createWorkingTaskStatus(params, "Processing task..."))
                                .build()
                );

                // Send final result
                Task taskResult = new Task.Builder()
                        .id(params.getMessage().getTaskId())
                        .contextId("ctx_" + UUID.randomUUID())
                        .status(createCompletedTaskStatus(params))
                        .history(List.of(params.getMessage()))
                        .artifacts(List.of(
                                createResultArtifact(result,
                                        params.getConfiguration() != null ?
                                                params.getConfiguration().getAcceptedOutputModes() : null)
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
                                    .taskId(params.getMessage().getTaskId())
                                    .contextId(ensureContextId(params.getMessage().getContextId()))
                                    .status(createFailedTaskStatus(params, e))
                                    .build()
                    );
                } catch (Exception sendError) {
                    logger.error("Error sending error event", sendError);
                }
            } finally {
                // Clean up thread-local storage
                a2aOutputEmitter.clearStreamId();
                streamingHandler.closeStream(streamId);
            }
        });

        return emitter;
    }

    private TaskStatus createWorkingTaskStatus(MessageSendParams params, String textPart) {
        return new TaskStatus(
                TaskState.WORKING,
                new Message.Builder()
                        .messageId(UUID.randomUUID().toString())
                        .role(Message.Role.AGENT)
                        .parts(List.of(new TextPart(textPart)))
                        .contextId(params.getMessage().getContextId())
                        .taskId(params.getMessage().getTaskId())
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
                        .contextId(params.getMessage().getContextId())
                        .taskId(params.getMessage().getTaskId())
                        .build(),
                LocalDateTime.now()
        );
    }

    private TaskStatus createFailedTaskStatus(MessageSendParams params, Exception e) {
        return new TaskStatus(
                TaskState.FAILED,
                new Message.Builder()
                        .messageId(UUID.randomUUID().toString())
                        .role(Message.Role.AGENT)
                        .parts(List.of(new TextPart("Error: " + e.getMessage())))
                        .contextId(params.getMessage().getContextId())
                        .taskId(params.getMessage().getTaskId())
                        .build(),
                LocalDateTime.now()
        );
    }

    private String ensureContextId(String providedContextId) {
        return providedContextId != null ? providedContextId : "ctx_" + UUID.randomUUID().toString();
    }

    private Artifact createResultArtifact(
            AgentProcessExecution result,
            List<String> acceptedOutputModes
    ) {
        return new Artifact.Builder()
                .artifactId(UUID.randomUUID().toString())
                .parts(List.of(new DataPart(Map.of("output", result.getOutput()))))
                .build();
    }
}