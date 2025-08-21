package org.tanzu.reviewer.agent;

import com.embabel.agent.channel.OutputChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AgentConfiguration {
    @Primary
    @Bean
    public OutputChannel responseOutputChannel() {
        return new ResponsePublisher();
    }
}
