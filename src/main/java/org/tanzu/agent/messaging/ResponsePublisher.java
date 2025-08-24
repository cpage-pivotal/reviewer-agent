package org.tanzu.agent.messaging;

import com.embabel.agent.channel.AssistantMessageOutputChannelEvent;
import com.embabel.agent.channel.OutputChannel;
import com.embabel.agent.channel.OutputChannelEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class ResponsePublisher implements OutputChannel {

    private static final Logger logger = LoggerFactory.getLogger(ResponsePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final AgentProcessCorrelationService correlationService;

    public ResponsePublisher(RabbitTemplate rabbitTemplate, AgentProcessCorrelationService correlationService) {
        this.rabbitTemplate = rabbitTemplate;
        this.correlationService = correlationService;
    }

    @Override
    public void send(@NotNull OutputChannelEvent event) {
        if (event instanceof AssistantMessageOutputChannelEvent messageEvent) {

            // Extract event details
            String processId = messageEvent.getProcessId();
            String content = messageEvent.getContent();
            String name = messageEvent.getName();

            logger.info("Processing AssistantMessageOutputChannelEvent for process: {}, name: {}", processId, name);

            // Get the correlation ID for this process
            String correlationId = correlationService.getCorrelationId(processId);

            if (correlationId != null) {
                // Send reply message to RabbitMQ with proper completion flags
                sendReplyMessage(content, correlationId, processId, name);
            } else {
                logger.warn("No correlation ID found for process {}, cannot send RabbitMQ reply", processId);
                // Still log the event for debugging
                logger.debug("Event content: {}, name: {}", content, name);
            }
        } else {
            logger.debug("Received non-AssistantMessageOutputChannelEvent: {}", event.getClass().getSimpleName());
        }
    }

    /**
     * Send a reply message to the RabbitMQ reply queue.
     * Decodes completion flags that were set by the agent in the action name.
     * ResponsePublisher does not decide completion - it only passes through agent's decision.
     */
    private void sendReplyMessage(String content, String correlationId, String processId, String name) {
        try {
            // Decode completion flag from enhanced action name set by the agent
            String actionName = name;
            boolean isComplete = true;

            if (name != null && name.contains("|complete:")) {
                // Parse enhanced action name: "craftStory|complete:false"
                String[] parts = name.split("\\|");
                actionName = parts[0]; // Extract original action name

                for (String part : parts) {
                    if (part.startsWith("complete:")) {
                        isComplete = Boolean.parseBoolean(part.substring("complete:".length()));
                        break;
                    }
                }

                logger.debug("Decoded completion flag from agent for action '{}': complete={}",
                        actionName, isComplete);
            } else {
                // Legacy action name without completion info - use default
                logger.debug("Using default completion flag for action '{}': complete={}",
                        actionName, isComplete);
            }

            // Create the reply message with agent-determined flag
            AgentResponse replyMessage = new AgentResponse(content, correlationId, processId, actionName);

            // Set the completion flag as determined by the agent
            replyMessage.setIsComplete(isComplete);
            replyMessage.setAgentType("reviewer");

            logger.info("Sending reply message to queue for correlationId: {}, action: {}, isComplete: {}",
                    correlationId, actionName, isComplete);
            logger.debug("Reply message content: {}", replyMessage);

            // Send the message to the reply queue with correlation ID in headers
            rabbitTemplate.convertAndSend(
                    RabbitMQConfiguration.REPLY_ROUTING_KEY,
                    replyMessage,
                    message -> {
                        MessageProperties properties = message.getMessageProperties();
                        properties.setCorrelationId(correlationId);
                        properties.setMessageId(processId);
                        return message;
                    }
            );

            logger.info("Successfully sent {} reply message for correlationId: {} and processId: {}",
                    isComplete ? "final" : "partial", correlationId, processId);

        } catch (Exception e) {
            logger.error("Error sending reply message for correlationId: {} and processId: {}",
                    correlationId, processId, e);
        }
    }
}