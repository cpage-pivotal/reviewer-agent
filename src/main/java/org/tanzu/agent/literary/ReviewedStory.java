package org.tanzu.agent.literary;

import com.embabel.agent.domain.library.HasContent;
import com.embabel.agent.prompt.persona.Persona;
import com.embabel.common.core.types.Timestamped;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

record ReviewedStory(
        Story story,
        String review,
        Persona reviewer
) implements HasContent, Timestamped {

    @Override
    @NonNull
    public Instant getTimestamp() {
        return Instant.now();
    }

    @Override
    @NonNull
    public String getContent() {
        return String.format("""
                        # Story
                        %s
                        
                        # Review
                        %s
                        
                        # Reviewer
                        %s, %s
                        """,
                story.text(),
                review,
                reviewer.getName(),
                getTimestamp().atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"))
        ).trim();
    }

    public String status() {
        return "Reviewed the story:\n\n" + review;
    }
}
