package com.example.websocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("roomId")
    private String roomId;
    
    @JsonProperty("senderId")
    private String senderId;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("timestamp")
    private Double timestamp; // Changed to Double to match client
    
    @JsonProperty("messageId")
    private String messageId;
    
    @JsonProperty("replyToMessageId")
    private String replyToMessageId;
    
    @JsonProperty("payload")
    private Map<String, String> payload;

    public ChatMessage() {}

    public ChatMessage(String type, String roomId, String senderId, String content, Instant timestamp) {
        this.type = type;
        this.roomId = roomId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp != null ? (double) timestamp.getEpochSecond() : null;
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Double getTimestamp() { return timestamp; }
    public void setTimestamp(Double timestamp) { this.timestamp = timestamp; }
    
    // Convenience method to get timestamp as Instant
    public Instant getTimestampAsInstant() {
        return timestamp != null ? Instant.ofEpochSecond(timestamp.longValue()) : null;
    }
    
    // Convenience method to set timestamp from Instant
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp != null ? (double) timestamp.getEpochSecond() : null;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public Map<String, String> getPayload() { return payload; }
    public void setPayload(Map<String, String> payload) { this.payload = payload; }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "type='" + type + '\'' +
                ", roomId='" + roomId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", messageId='" + messageId + '\'' +
                ", replyToMessageId='" + replyToMessageId + '\'' +
                ", payload=" + payload +
                '}';
    }
}