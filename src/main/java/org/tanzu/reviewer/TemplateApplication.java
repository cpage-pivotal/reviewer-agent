/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tanzu.reviewer;

import com.embabel.agent.config.annotation.EnableAgents;
import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
@EnableAgents
class TemplateApplication {
    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(AgentPlatform agentPlatform) {
        return args -> {
            for (Agent agent : agentPlatform.agents()) {
                System.out.println(agent);
                ProcessOptions processOptions = ProcessOptions.builder().build();
                AgentProcess agentProcess = agentPlatform.createAgentProcessFrom(agent,
                        processOptions,
                        new UserInput("Write a story about a risky poker game"));

                AgentProcess completedProcess = agentPlatform.start(agentProcess).get();
                System.out.println(completedProcess.statusReport());
            }
        };
    }
}