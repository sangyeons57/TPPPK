package com.example.websocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

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
    private Instant timestamp;

    public ChatMessage() {}

    public ChatMessage(String type, String roomId, String senderId, String content, Instant timestamp) {
        this.type = type;
        this.roomId = roomId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
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

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "type='" + type + '\'' +
                ", roomId='" + roomId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}