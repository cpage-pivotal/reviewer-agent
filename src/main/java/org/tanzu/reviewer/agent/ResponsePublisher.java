package org.tanzu.reviewer.agent;

import com.embabel.agent.channel.AssistantMessageOutputChannelEvent;
import com.embabel.agent.channel.OutputChannel;
import com.embabel.agent.channel.OutputChannelEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.tanzu.reviewer.messaging.AgentProcessCorrelationService;
import org.tanzu.reviewer.messaging.AgentResponse;
import org.tanzu.reviewer.messaging.RabbitMQConfiguration;

public class ResponsePublisher implements OutputChannel {

    private static final Logger logger = LoggerFactory.getLogger(ResponsePublisher.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AgentProcessCorrelationService correlationService;

    @Override
    public void send(@NotNull OutputChannelEvent event) {
        if (event instanceof AssistantMessageOutputChannelEvent messageEvent) {

            // Extract event details
            String processId = messageEvent.getProcessId();
            String content = messageEvent.getContent();
            String name = messageEvent.getName();

            logger.info("Processing AssistantMessageOutputChannelEvent for process: {}", processId);

            // Get the correlation ID for this process
            String correlationId = correlationService.getCorrelationId(processId);

            if (correlationId != null) {
                // Send reply message to RabbitMQ
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
     * Send a reply message to the RabbitMQ reply queue
     */
    private void sendReplyMessage(String content, String correlationId, String processId, String name) {
        try {
            // Create the reply message
            AgentResponse replyMessage = new AgentResponse(content, correlationId, processId, name);

            logger.info("Sending reply message to queue for correlationId: {}", correlationId);
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

            logger.info("Successfully sent reply message for correlationId: {} and processId: {}",
                    correlationId, processId);

        } catch (Exception e) {
            logger.error("Error sending reply message for correlationId: {} and processId: {}",
                    correlationId, processId, e);
        }
    }
}