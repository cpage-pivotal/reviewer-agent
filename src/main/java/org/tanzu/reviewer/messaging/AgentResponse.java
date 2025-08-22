package org.tanzu.reviewer.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AgentResponse {

    @JsonProperty("content")
    private String content;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("processId")
    private String processId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("status")
    private String status;

    // NEW FIELDS to match AgentResponse expectation
    @JsonProperty("agentType")
    private String agentType;

    @JsonProperty("isPartial")
    private Boolean isPartial;

    @JsonProperty("isComplete")
    private Boolean isComplete;

    @JsonProperty("metadata")
    private Object metadata;

    public AgentResponse() {
        this.timestamp = System.currentTimeMillis();
        this.status = "success";
        // Set defaults for new fields
        this.agentType = "reviewer";
        this.isPartial = false;
        this.isComplete = true;
        this.metadata = null;
    }

    public AgentResponse(String content, String correlationId, String processId, String name) {
        this();
        this.content = content;
        this.correlationId = correlationId;
        this.processId = processId;
        this.name = name;
    }

    // Existing getters and setters...
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // NEW GETTERS AND SETTERS for AgentResponse compatibility
    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public Boolean getIsPartial() {
        return isPartial;
    }

    public void setIsPartial(Boolean isPartial) {
        this.isPartial = isPartial;
    }

    public Boolean getIsComplete() {
        return isComplete;
    }

    public void setIsComplete(Boolean isComplete) {
        this.isComplete = isComplete;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "AgentReplyMessage{" +
                "content='" + content + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", processId='" + processId + '\'' +
                ", name='" + name + '\'' +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                ", agentType='" + agentType + '\'' +
                ", isPartial=" + isPartial +
                ", isComplete=" + isComplete +
                ", metadata=" + metadata +
                '}';
    }
}