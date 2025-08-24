package org.tanzu.agent.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AgentRequest {

    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("timestamp")
    private long timestamp;

    public AgentRequest() {
        this.timestamp = System.currentTimeMillis();
    }

    public AgentRequest(String prompt, String correlationId) {
        this();
        this.prompt = prompt;
        this.correlationId = correlationId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AgentRequestMessage{" +
                "prompt='" + prompt + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
