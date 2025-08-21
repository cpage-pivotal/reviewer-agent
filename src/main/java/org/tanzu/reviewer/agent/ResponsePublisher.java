package org.tanzu.reviewer.agent;

import com.embabel.agent.channel.AssistantMessageOutputChannelEvent;
import com.embabel.agent.channel.OutputChannel;
import com.embabel.agent.channel.OutputChannelEvent;
import org.jetbrains.annotations.NotNull;

public class ResponsePublisher implements OutputChannel {
    @Override
    public void send(@NotNull OutputChannelEvent event) {
        if (event instanceof AssistantMessageOutputChannelEvent messageEvent) {

            // Publish externally - your custom logic here
            String processId = messageEvent.getProcessId();
            String content = messageEvent.getContent();
            String name = messageEvent.getName();
        }
    }
}

