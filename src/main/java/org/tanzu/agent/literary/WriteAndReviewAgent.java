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
package org.tanzu.agent.literary;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Export;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.common.PromptRunner;
import com.embabel.agent.channel.AssistantMessageOutputChannelEvent;
import com.embabel.agent.config.models.OpenAiModels;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.common.ai.model.LlmOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;

@Agent(description = "Generate a story based on user input and review it")
@Profile("!test")
public class WriteAndReviewAgent {

    private final int storyWordCount;
    private final int reviewWordCount;

    WriteAndReviewAgent(
            @Value("${storyWordCount:100}") int storyWordCount,
            @Value("${reviewWordCount:100}") int reviewWordCount
    ) {
        this.storyWordCount = storyWordCount;
        this.reviewWordCount = reviewWordCount;
    }

    @AchievesGoal(
            description = "The story has been crafted and reviewed by a book reviewer",
            export = @Export(remote = true, name = "writeAndReviewStory"))
    @Action
    ReviewedStory reviewStory(UserInput userInput, Story story, OperationContext context) {
        String review = context.promptRunner()
                .withLlm(LlmOptions.fromModel(OpenAiModels.GPT_41_MINI))
                .withPromptContributor(Personas.REVIEWER)
                .generateText(String.format("""
                                You will be given a short story to review.
                                Review it in %d words or less.
                                Consider whether or not the story is engaging, imaginative, and well-written.
                                Also consider whether the story is appropriate given the original user input.
                                
                                # Story
                                %s
                                
                                # User input that inspired the story
                                %s
                                """,
                        reviewWordCount,
                        story.text(),
                        userInput.getContent()
                ).trim());

        ReviewedStory result = new ReviewedStory(
                story,
                review,
                Personas.REVIEWER
        );

        sendUpdate(context, result.status(), "reviewStory", true);
        return result;
    }

    @Action
    Story craftStory(UserInput userInput, OperationContext context) {

        PromptRunner runner = context.promptRunner()
                // Higher temperature for more creative output
                .withLlm(LlmOptions.fromModel(OpenAiModels.GPT_41_MINI, 0.9))
                .withPromptContributor(Personas.WRITER);

        Story result = runner.createObject(String.format("""
                        Craft a short story in %d words or less.
                        The story should be engaging and imaginative.
                        Use the user's input as inspiration if possible.
                        If the user has provided a name, include it in the story.
                        
                        # User input
                        %s
                        """,
                storyWordCount,
                userInput.getContent()
        ).trim(), Story.class);

        sendUpdate(context, result.status(), "craftStory", false);
        return result;
    }

    /**
     * Send an update with explicit completion flag encoded in the action name.
     * Only the agent knows when it's sending the final message.
     *
     * @param context Operation context
     * @param content Message content
     * @param action Action name for identification
     * @param isComplete True if this is the final response in the sequence
     */
    private static void sendUpdate(OperationContext context, String content, String action,
                                   boolean isComplete) {

        // Encode completion information in the action name for ResponsePublisher to decode
        String enhancedActionName = String.format("%s|complete:%s", action, isComplete);

        context.getProcessContext().getOutputChannel().send(
                new AssistantMessageOutputChannelEvent(
                        context.getProcessContext().getAgentProcess().getId(),
                        content,
                        enhancedActionName
                )
        );
    }
}