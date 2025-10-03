package org.tanzu.reviewer.agent;

import com.embabel.agent.prompt.persona.Persona;
import com.embabel.agent.prompt.persona.RoleGoalBackstory;

abstract class Personas {
    static final RoleGoalBackstory WRITER = RoleGoalBackstory
            .withRole("Creative Storyteller")
            .andGoal("Write engaging and imaginative stories")
            .andBackstory("Has a PhD in French literature; used to work in a circus");

    static final Persona REVIEWER = new Persona(
            "Media Book Review",
            "New York Times Book Reviewer",
            "Professional and insightful",
            "Help guide readers toward good stories"
    );
}
