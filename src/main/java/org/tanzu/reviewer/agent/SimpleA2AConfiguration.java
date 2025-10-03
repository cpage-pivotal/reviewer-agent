package org.tanzu.reviewer.agent;

import com.embabel.agent.core.ProcessOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Configuration to register the A2AOutputEmitter as a default listener
 * for all agent processes when running as an A2A server.
 *
 * This is a simpler alternative to creating a custom request handler.
 * The listener will automatically emit Story and ReviewedStory outputs
 * as A2A messages when they are bound during action execution.
 */
@Configuration
@Profile("a2a")
public class SimpleA2AConfiguration {

    /**
     * Configure default ProcessOptions with the A2AOutputEmitter listener.
     * This will be picked up by the Autonomy service if configured properly.
     */
    @Bean
    public ProcessOptions.Builder defaultProcessOptionsBuilder(A2AOutputEmitter a2aOutputEmitter) {
        return new ProcessOptions.Builder()
                .withListeners(List.of(a2aOutputEmitter));
    }
}