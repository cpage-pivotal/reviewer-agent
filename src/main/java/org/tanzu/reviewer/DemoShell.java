package org.tanzu.reviewer;

import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.domain.io.UserInput;
import org.tanzu.reviewer.agent.WriteAndReviewAgent;
import org.tanzu.reviewer.injected.InjectedDemo;

//@ShellComponent
record DemoShell(InjectedDemo injectedDemo, AgentPlatform agentPlatform) {

//    @ShellMethod("Demo")
    String demo() {
        // Illustrate calling an agent programmatically,
        // as most often occurs in real applications.
        var reviewedStory = AgentInvocation
                .create(agentPlatform, WriteAndReviewAgent.ReviewedStory.class)
                .invoke(new UserInput("Tell me a story about caterpillars"));
        return reviewedStory.getContent();
    }

//    @ShellMethod("Invent an animal")
    String animal() {
        return injectedDemo.inventAnimal().toString();
    }
}
