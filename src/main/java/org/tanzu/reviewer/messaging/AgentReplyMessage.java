package org.tanzu.reviewer.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AgentReplyMessage {

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

    public AgentReplyMessage() {
        this.timestamp = System.currentTimeMillis();
        this.status = "success";
    }

    public AgentReplyMessage(String content, String correlationId, String processId, String name) {
        this();
        this.content = content;
        this.correlationId = correlationId;
        this.processId = processId;
        this.name = name;
    }

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

    @Override
    public String toString() {
        return "AgentReplyMessage{" +
                "content='" + content + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", processId='" + processId + '\'' +
                ", name='" + name + '\'' +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                '}';
    }
}