package org.tanzu.reviewer.agent;

import com.embabel.agent.a2a.server.support.A2AStreamingHandler;
import com.embabel.agent.event.AgentProcessEvent;
import com.embabel.agent.event.AgenticEventListener;
import com.embabel.agent.event.ObjectBindingEvent;
import io.a2a.spec.Artifact;
import io.a2a.spec.DataPart;
import io.a2a.spec.TaskArtifactUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listens to agent process events and emits Story and ReviewedStory outputs
 * as A2A TaskArtifactUpdateEvents for streaming to A2A clients.
 */
@Component
@Profile("a2a")
public class A2AOutputEmitter implements AgenticEventListener {

    private static final Logger logger = LoggerFactory.getLogger(A2AOutputEmitter.class);

    private final A2AStreamingHandler streamingHandler;

    // Thread-local storage for current stream ID
    private final ThreadLocal<String> currentStreamId = new ThreadLocal<>();

    public A2AOutputEmitter(A2AStreamingHandler streamingHandler) {
        this.streamingHandler = streamingHandler;
    }

    /**
     * Call this from your request handler to set the stream ID for the current request
     */
    public void setStreamId(String streamId) {
        currentStreamId.set(streamId);
    }

    /**
     * Clear the stream ID after the request is complete
     */
    public void clearStreamId() {
        currentStreamId.remove();
    }

    @Override
    public void onProcessEvent(AgentProcessEvent event) {
        // Only handle binding events for our target types
        if (!(event instanceof ObjectBindingEvent bindingEvent)) {
            return;
        }

        String streamId = currentStreamId.get();
        if (streamId == null) {
            logger.debug("No stream ID set, skipping A2A emission for {}",
                    bindingEvent.getValue().getClass().getSimpleName());
            return;
        }

        Object value = bindingEvent.getValue();

        if (value instanceof WriteAndReviewAgent.Story story) {
            logger.info("Emitting Story as A2A artifact for stream {}", streamId);
            emitArtifact(streamId, "story", value, Map.of(
                    "text", story.text(),
                    "type", "story"
            ));
        } else if (value instanceof WriteAndReviewAgent.ReviewedStory reviewedStory) {
            logger.info("Emitting ReviewedStory as A2A artifact for stream {}", streamId);
            emitArtifact(streamId, "reviewed_story", value, Map.of(
                    "story", reviewedStory.story().text(),
                    "review", reviewedStory.review(),
                    "reviewer", reviewedStory.reviewer().getName(),
                    "type", "reviewed_story"
            ));
        }
    }

    private void emitArtifact(
            String streamId,
            String artifactType,
            Object value,
            Map<String, Object> data
    ) {
        try {
            Artifact artifact = new Artifact.Builder()
                    .artifactId(UUID.randomUUID().toString())
                    .parts(List.of(new DataPart(data)))
                    .build();

            TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
                    .artifact(artifact)
                    .build();

            streamingHandler.sendStreamEvent(streamId, event);
            logger.debug("Successfully emitted {} artifact to stream {}", artifactType, streamId);
        } catch (Exception e) {
            logger.error("Failed to emit {} artifact", artifactType, e);
        }
    }
}