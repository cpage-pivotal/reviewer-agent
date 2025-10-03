# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Embabel-based Spring Boot application that demonstrates agent development with LLM integration. The project showcases multi-step agent workflows using the Embabel framework (v0.1.3), which provides high-level abstractions over Spring AI for building LLM-powered agents.

**Key Technologies:**
- Java 21
- Spring Boot 3.5.6
- Embabel Agent Framework
- Spring AI (OpenAI integration by default)
- A2A (Agent-to-Agent) protocol support
- Maven build system

## Core Architecture

### Agent Pattern

Agents in Embabel are Spring-managed beans annotated with `@Agent`. They contain `@Action` methods that define discrete steps in a workflow. The framework automatically chains actions together based on parameter types.

**Example workflow (WriteAndReviewAgent):**
1. `craftStory()` takes `UserInput` → produces `Story`
2. `reviewStory()` takes `UserInput` + `Story` → produces `ReviewedStory`

The method marked with `@AchievesGoal` is the final goal. Embabel uses action chaining to determine execution order automatically.

### Key Components

- **Agents** (`src/main/java/org/tanzu/reviewer/agent/`): Core agent implementations
  - `@Agent`: Marks a class as an Embabel agent
  - `@Action`: Defines a step in the agent workflow
  - `@AchievesGoal`: Marks the final goal method

- **Personas** (`Personas.java`): Define LLM behaviors using `RoleGoalBackstory` or `Persona` classes
  - Attached to prompts via `withPromptContributor()`

- **Injected Components** (`src/main/java/org/tanzu/reviewer/injected/`): Spring beans that use Embabel's `Ai` interface directly for simpler LLM interactions without full agent workflow

- **A2A Integration**: Event-driven architecture for streaming agent outputs
  - `A2AOutputEmitter`: Listens to `ObjectBindingEvent` and emits artifacts to A2A clients
  - Activated via `spring.profiles.active=a2a` (currently the default profile)

### LLM Configuration

LLM selection happens in `OperationContext.ai()` via:
- `withAutoLlm()`: Uses framework's default model selection
- `withDefaultLlm()`: Uses configured default
- `withLlm(LlmOptions.withModel(...))`: Explicit model selection
- `withLlm(LlmOptions.withLlmForRole("role-name"))`: Uses role-based configuration from application.properties

Temperature, top-p, and other parameters are set via `LlmOptions.withTemperature()`, etc.

## Common Commands

### Build
```bash
mvn clean install
```

### Run Tests
```bash
mvn test
```

### Run Single Test
```bash
mvn test -Dtest=WriteAndReviewAgentTest#testWriteAndReviewAgent
```

### Start Application (Spring Shell)
```bash
./scripts/shell.sh
```

Once the shell starts:
- `x "Tell me a story about..."`: Invokes the agent dynamically based on user input
- `demo`: Programmatically invokes WriteAndReviewAgent with hardcoded input
- `animal`: Demonstrates injected `Ai` usage via InjectedDemo

## Testing Agents

Use Embabel's testing utilities:
- `FakeOperationContext.create()`: Mock context for unit tests
- `context.expectResponse(object)`: Stub LLM responses
- `context.getLlmInvocations()`: Verify prompts sent to LLM
- `FakePromptRunner`: Access to prompt history

Example pattern:
```java
var context = FakeOperationContext.create();
context.expectResponse(new Story("Once upon a time..."));
agent.craftStory(userInput, context);
var prompt = context.getLlmInvocations().getFirst().getPrompt();
assertTrue(prompt.contains("expected-term"));
```

## Configuration Notes

- `application.properties`: Configure default LLMs, LLM roles, embedding models
- Active profile: `a2a` (enables A2A streaming output)
- Configurable word counts: `storyWordCount` and `reviewWordCount` properties
- OpenAI is the default provider (uncomment Anthropic dependency in pom.xml to switch)

## Important Patterns

1. **Action Chaining**: Embabel determines action execution order by matching return types to parameter types
2. **Records as Data Types**: Use Java records for agent data structures (Story, ReviewedStory, etc.)
3. **OperationContext**: Always pass `OperationContext` as final parameter in `@Action` methods
4. **Export Remote**: `@Export(remote = true)` on `@AchievesGoal` enables remote invocation
5. **Profile Exclusion**: Use `@Profile("!test")` to prevent agent instantiation during tests

## Development Notes

- Agent classes are Spring beans - they can inject any Spring-managed dependencies
- Use `HasContent` interface for objects that should have a formatted text representation
- Use `Timestamped` interface for objects that need timestamps
- A2A streaming requires thread-local stream ID management (see A2AOutputEmitter)
