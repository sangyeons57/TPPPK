package com.example.websocket.model;

import com.example.websocket.constants.WebSocketEventConstants;
import com.example.websocket.constants.PayloadConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {
    @JsonProperty(WebSocketEventConstants.FIELD_TYPE)
    private String type;
    
    @JsonProperty(WebSocketEventConstants.FIELD_ROOM_ID)
    private String roomId;
    
    @JsonProperty(WebSocketEventConstants.FIELD_SENDER_ID)
    private String senderId;
    
    @JsonProperty(WebSocketEventConstants.FIELD_MESSAGE_TYPE)
    private String messageType = WebSocketEventConstants.MESSAGE_TYPE_TEXT; // Default to TEXT type
    
    @JsonProperty(WebSocketEventConstants.FIELD_TIMESTAMP)
    private Double timestamp; // Changed to Double to match client
    
    @JsonProperty(WebSocketEventConstants.FIELD_MESSAGE_ID)
    private String messageId;
    
    @JsonProperty(WebSocketEventConstants.FIELD_REPLY_TO_MESSAGE_ID)
    private String replyToMessageId;
    
    @JsonProperty(WebSocketEventConstants.FIELD_PAYLOAD)
    private Map<String, Object> payload;
    
    @JsonProperty(WebSocketEventConstants.FIELD_PROJECT_ID)
    private String projectId;
    
    @JsonProperty(WebSocketEventConstants.FIELD_CHANNEL_TYPE)
    private String channelType;

    public ChatMessage() {}

    public ChatMessage(String type, String roomId, String senderId, Map<String, Object> payload, Instant timestamp) {
        this.type = type;
        this.roomId = roomId;
        this.senderId = senderId;
        this.payload = payload;
        this.setTimestampFromInstant(timestamp);
    }

    // Factory methods for payload-based messages
    public static ChatMessage createWithTextPayload(String type, String roomId, String senderId, String messageType, String textContent, Instant timestamp) {
        ChatMessage message = new ChatMessage();
        message.type = type;
        message.roomId = roomId;
        message.senderId = senderId;
        message.messageType = messageType != null ? messageType : WebSocketEventConstants.MESSAGE_TYPE_TEXT;
        
        // Create payload
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put(PayloadConstants.CONTENT, textContent != null ? textContent : "");
        message.payload = payloadMap;
        message.setTimestampFromInstant(timestamp);
        
        return message;
    }
    
    public static ChatMessage createSystemMessage(String type, String roomId, String senderId, String systemText, Instant timestamp) {
        return createWithTextPayload(type, roomId, senderId, WebSocketEventConstants.MESSAGE_TYPE_SYSTEM, systemText, timestamp);
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    // Content getter/setter - extracts from payload
    public String getContent() { 
        if (payload != null && payload.containsKey(PayloadConstants.CONTENT)) {
            Object contentObj = payload.get(PayloadConstants.CONTENT);
            return contentObj != null ? contentObj.toString() : null;
        }
        return null;
    }
    
    public void setContent(String content) { 
        if (payload == null) {
            payload = new HashMap<>();
        }
        payload.put(PayloadConstants.CONTENT, content);
    }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public Double getTimestamp() { return timestamp; }
    public void setTimestamp(Double timestamp) { this.timestamp = timestamp; }
    
    // Convenience method to get timestamp as Instant
    public Instant getTimestampAsInstant() {
        return timestamp != null ? Instant.ofEpochSecond(timestamp.longValue()) : null;
    }
    
    // Convenience method to set timestamp from Instant
    public void setTimestampFromInstant(Instant instant) {
        this.timestamp = instant != null ? (double) instant.getEpochSecond() : null;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getChannelType() { return channelType; }
    public void setChannelType(String channelType) { this.channelType = channelType; }

    /**
     * Returns a one-line summary of all fields for logging/debugging.
     */
    public String toSummaryString() {
        return String.format(
            "type='%s', roomId='%s', senderId='%s', messageType='%s', payload=%s, timestamp=%s, messageId='%s', replyToMessageId='%s', projectId='%s', channelType='%s'",
            String.valueOf(type),
            String.valueOf(roomId),
            String.valueOf(senderId),
            String.valueOf(messageType),
            payload != null ? payload.toString() : "null",
            String.valueOf(timestamp),
            String.valueOf(messageId),
            String.valueOf(replyToMessageId),
            String.valueOf(projectId),
            String.valueOf(channelType)
        );
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "type='" + type + '\'' +
                ", roomId='" + roomId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", payload=" + payload +
                ", timestamp=" + timestamp +
                ", messageId='" + messageId + '\'' +
                ", replyToMessageId='" + replyToMessageId + '\'' +
                ", projectId='" + projectId + '\'' +
                ", channelType='" + channelType + '\'' +
                '}';
    }
}