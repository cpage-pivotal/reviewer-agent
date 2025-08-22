package org.tanzu.reviewer.messaging;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Listens for agent request messages and processes them using the AgentPlatform
 */
@Service
public class AgentRequestListener {

    private static final Logger logger = LoggerFactory.getLogger(AgentRequestListener.class);

    private final AgentPlatform agentPlatform;
    private final AgentProcessCorrelationService correlationService;

    // Store correlation mappings for active processes
    private final ConcurrentHashMap<String, String> processToCorrelationMap = new ConcurrentHashMap<>();

    @Autowired
    public AgentRequestListener(AgentPlatform agentPlatform,
                                AgentProcessCorrelationService correlationService) {
        this.agentPlatform = agentPlatform;
        this.correlationService = correlationService;
    }

    /**
     * Listens for messages on the reviewer.agent.request queue
     */
    @RabbitListener(queues = RabbitMQConfiguration.REQUEST_QUEUE)
    public void handleAgentRequest(
            @Payload AgentRequest requestMessage,
            @Header(value = "correlationId", required = false) String headerCorrelationId) {

        logger.info("Received agent request: {}", requestMessage);

        // Use correlationId from message payload, fallback to header
        String correlationId = requestMessage.getCorrelationId() != null
                ? requestMessage.getCorrelationId()
                : headerCorrelationId;

        if (correlationId == null) {
            logger.warn("No correlation ID found in request message or headers");
            return;
        }

        try {
            // Create UserInput from the prompt
            UserInput userInput = new UserInput(requestMessage.getPrompt(), Instant.now());

            // Process the request asynchronously
            processAgentRequestAsync(userInput, correlationId);

        } catch (Exception e) {
            logger.error("Error processing agent request with correlationId: {}", correlationId, e);
            // Could send error response here if needed
        }
    }

    /**
     * Process the agent request asynchronously
     */
    private void processAgentRequestAsync(UserInput userInput, String correlationId) {
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting agent process for correlationId: {}", correlationId);

                // Find the first available agent (in this case, the WriteAndReviewAgent)
                Agent agent = agentPlatform.agents().stream()
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No agents available"));

                // Create process options
                ProcessOptions processOptions = ProcessOptions.builder().build();

                // Create the agent process
                AgentProcess agentProcess = agentPlatform.createAgentProcessFrom(
                        agent,
                        processOptions,
                        userInput
                );

                // Store the correlation mapping
                correlationService.associateProcessWithCorrelation(
                        agentProcess.getId(),
                        correlationId
                );

                logger.info("Created agent process {} for correlationId: {}",
                        agentProcess.getId(), correlationId);

                // Start the agent process
                AgentProcess completedProcess = agentPlatform.start(agentProcess).get();

                logger.info("Completed agent process {} for correlationId: {}",
                        completedProcess.getId(), correlationId);

            } catch (Exception e) {
                logger.error("Error in async agent processing for correlationId: {}",
                        correlationId, e);
            }
        });
    }
}
