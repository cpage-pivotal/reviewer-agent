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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listens to agent process events and emits Story and ReviewedStory outputs
 * as A2A TaskArtifactUpdateEvents for streaming to A2A clients.
 *
 * For streaming requests, artifacts are sent immediately via the streaming handler.
 * For non-streaming requests, artifacts are collected and can be retrieved at the end.
 */
@Component
@Profile("a2a")
public class A2AOutputEmitter implements AgenticEventListener {

    private static final Logger logger = LoggerFactory.getLogger(A2AOutputEmitter.class);

    private final A2AStreamingHandler streamingHandler;

    // Thread-local storage for current stream ID (streaming requests only)
    private final ThreadLocal<String> currentStreamId = new ThreadLocal<>();

    // Thread-local storage for collecting artifacts (non-streaming requests)
    private final ThreadLocal<List<Artifact>> collectedArtifacts = new ThreadLocal<>();

    public A2AOutputEmitter(A2AStreamingHandler streamingHandler) {
        this.streamingHandler = streamingHandler;
    }

    /**
     * Call this from your streaming request handler to set the stream ID for the current request
     */
    public void setStreamId(String streamId) {
        currentStreamId.set(streamId);
        logger.debug("Set stream ID: {}", streamId);
    }

    /**
     * Call this from your non-streaming request handler to start collecting artifacts
     */
    public void startCollecting() {
        collectedArtifacts.set(new ArrayList<>());
        logger.debug("Started collecting artifacts for non-streaming request");
    }

    /**
     * Get the collected artifacts (for non-streaming requests)
     */
    public List<Artifact> getCollectedArtifacts() {
        List<Artifact> artifacts = collectedArtifacts.get();
        return artifacts != null ? new ArrayList<>(artifacts) : List.of();
    }

    /**
     * Clear the stream ID and collected artifacts after the request is complete
     */
    public void clear() {
        String streamId = currentStreamId.get();
        List<Artifact> artifacts = collectedArtifacts.get();

        if (streamId != null) {
            logger.debug("Clearing stream ID: {}", streamId);
        }
        if (artifacts != null) {
            logger.debug("Clearing {} collected artifacts", artifacts.size());
        }

        currentStreamId.remove();
        collectedArtifacts.remove();
    }

    @Override
    public void onProcessEvent(AgentProcessEvent event) {
        // Only handle binding events for our target types
        if (!(event instanceof ObjectBindingEvent bindingEvent)) {
            return;
        }

        Object value = bindingEvent.getValue();

        // Process Story
        if (value instanceof WriteAndReviewAgent.Story story) {
            logger.info("Processing Story binding event");
            emitArtifact("story", value, Map.of(
                    "text", story.text(),
                    "type", "story"
            ));
        }
        // Process ReviewedStory
        else if (value instanceof WriteAndReviewAgent.ReviewedStory reviewedStory) {
            logger.info("Processing ReviewedStory binding event");
            emitArtifact("reviewed_story", value, Map.of(
                    "story", reviewedStory.story().text(),
                    "review", reviewedStory.review(),
                    "reviewer", reviewedStory.reviewer().getName(),
                    "type", "reviewed_story"
            ));
        }
    }

    private void emitArtifact(String artifactType, Object value, Map<String, Object> data) {
        // Create the artifact
        Artifact artifact = new Artifact.Builder()
                .artifactId(UUID.randomUUID().toString())
                .parts(List.of(new DataPart(data)))
                .build();

        // For non-streaming requests: collect the artifact
        List<Artifact> collecting = collectedArtifacts.get();
        if (collecting != null) {
            collecting.add(artifact);
            logger.info("Collected {} artifact for non-streaming request (total: {})",
                    artifactType, collecting.size());
        }

        // For streaming requests: send immediately
        String streamId = currentStreamId.get();
        if (streamId != null) {
            try {
                TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
                        .artifact(artifact)
                        .build();

                streamingHandler.sendStreamEvent(streamId, event);
                logger.info("Emitted {} artifact to stream {}", artifactType, streamId);
            } catch (Exception e) {
                logger.error("Failed to emit {} artifact to stream", artifactType, e);
            }
        } else if (collecting == null) {
            // Neither streaming nor collecting - this shouldn't happen if properly configured
            logger.warn("No stream ID or artifact collection active, artifact will be lost: {}",
                    artifactType);
        }
    }
}