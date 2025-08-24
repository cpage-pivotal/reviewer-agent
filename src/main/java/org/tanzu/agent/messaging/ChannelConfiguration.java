package org.tanzu.agent.messaging;

import com.embabel.agent.channel.OutputChannel;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChannelConfiguration {
    @Primary
    @Bean
    public OutputChannel responseOutputChannel(RabbitTemplate rabbitTemplate, AgentProcessCorrelationService correlationService) {
        return new ResponsePublisher(rabbitTemplate, correlationService);
    }
}
