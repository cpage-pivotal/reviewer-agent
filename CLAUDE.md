# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Core Maven Commands
- `./mvnw compile` - Compile the project
- `./mvnw test` - Run unit tests
- `./mvnw spring-boot:run` - Run the application locally
- `./mvnw clean package` - Build the JAR package
- `./mvnw clean install` - Full build with tests

### Development Tools
- `./scripts/shell.sh` - Run the application in interactive shell mode
- `./scripts/shell.cmd` - Windows equivalent for interactive mode

## Project Architecture

This is a Spring Boot application that implements an AI agent system using the Embabel Agent framework with RabbitMQ messaging integration.

### Core Components

**Main Application (`TemplateApplication.java`)**
- Entry point with `@EnableAgents` annotation to activate the Embabel agent framework
- Located in `org.tanzu.reviewer` package

**Agent Implementation (`WriteAndReviewAgent.java`)**
- Primary agent that generates stories and reviews them
- Two main actions: `craftStory()` and `reviewStory()`
- Uses configurable word counts via `storyWordCount` and `reviewWordCount` properties
- Implements creative writing with higher temperature (0.9) for story generation
- Exports functionality remotely as `writeAndReviewStory`

**RabbitMQ Messaging System**
- `RabbitMQConfiguration.java` - Defines queues, exchanges, and bindings
- `AgentRequestListener.java` - Handles incoming requests from `agent.reviewer.request` queue
- `AgentProcessCorrelationService.java` - Manages correlation between processes and external requests
- `ResponsePublisher.java` - Publishes results to `agent.reviewer.reply` queue
- Uses Jackson JSON message conversion for serialization

**Message Flow**
1. External systems send `AgentRequestMessage` to request queue
2. `AgentRequestListener` processes requests asynchronously
3. Creates `AgentProcess` using the `WriteAndReviewAgent`
4. Results published as `AgentReplyMessage` to reply queue
5. Correlation IDs track request-response pairs

### Key Dependencies
- Spring Boot 3.5.4 with Java 21
- Embabel Agent Framework (0.1.0) - proprietary AI agent platform
- Spring AMQP for RabbitMQ integration
- Custom repositories: `repo.embabel.com/artifactory/`

### Configuration
- RabbitMQ settings in `application.properties` (localhost:5672 by default)
- Agent behavior configured via `storyWordCount` and `reviewWordCount` properties
- Connection timeouts, retry policies, and concurrency limits configured
- Debug logging enabled for messaging components

### Testing
- Test classes in `src/test/java/org/tanzu/reviewer/agent/`
- Embabel test framework available via `embabel-agent-test` dependency
- Agent excluded from test profile via `@Profile("!test")`

The application serves as a bridge between external messaging systems and the Embabel AI agent platform, specifically for creative writing and review workflows.