package org.tanzu.agent.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfiguration {

    public static final String REQUEST_QUEUE = "reviewer.request";
    public static final String REPLY_QUEUE = "reviewer.reply";
    public static final String EXCHANGE_NAME = "agents";
    public static final String REQUEST_ROUTING_KEY = "reviewer.request";
    public static final String REPLY_ROUTING_KEY = "reviewer.reply";

    @Bean
    public DirectExchange reviewerAgentExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue requestQueue() {
        return QueueBuilder.durable(REQUEST_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5 minutes TTL
                .build();
    }

    @Bean
    public Queue replyQueue() {
        return QueueBuilder.durable(REPLY_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5 minutes TTL
                .build();
    }

    @Bean
    public Binding requestQueueBinding() {
        return BindingBuilder
                .bind(requestQueue())
                .to(reviewerAgentExchange())
                .with(REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding replyQueueBinding() {
        return BindingBuilder
                .bind(replyQueue())
                .to(reviewerAgentExchange())
                .with(REPLY_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setExchange(EXCHANGE_NAME);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        return factory;
    }
}