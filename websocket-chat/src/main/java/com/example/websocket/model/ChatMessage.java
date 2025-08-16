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
    /**
     * WebSocket 봉투(envelope) + (하위호환용) 중첩 메시지 구조를 모두 지원하는 서버 측 DTO.
     *
     * 도메인 Message ← ChatMessage 매핑 규칙(중요):
     * - Message.id               ← message.id (우선) / (폴백) messageId
     * - Message.senderId         ← message.senderId (우선) / (폴백) senderId
     * - Message.messageType      ← message.messageType (우선) / (폴백) messageType (없으면 TEXT 등 기본값)
     * - Message.payload          ← message.payload (우선) / (폴백) payload
     * - Message.replyToMessageId ← message.replyToMessageId (우선) / (폴백) replyToMessageId
     * - Message.createdAt        ← (message.timestamp 또는 timestamp) epoch seconds
     * - Message.channelId        ← roomId (봉투)
     * - Message.projectId        ← projectId (봉투, 선택)
     *
     * 전송/수신 원칙:
     * - 클라이언트는 가능하면 평탄(Flat) 스키마를 사용: 봉투에 모든 필드(messageId, messageType, payload, senderId, replyToMessageId, timestamp)를 포함.
     * - 서버는 중첩(message.*)을 우선 읽되, 없으면 봉투로 폴백하여 하위호환 유지.
     */
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
    

    // Nested domain message wrapper (하위호환용, 수신 시 우선 참조)
    @JsonProperty(WebSocketEventConstants.FIELD_MESSAGE)
    private MessageData message;

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


    public MessageData getMessage() { return message; }
    public void setMessage(MessageData message) { this.message = message; }

    // Effective getters: nested(message.*) 우선, 없으면 봉투(envelope) 폴백
    public String getEffectiveMessageId() {
        if (message != null && message.getId() != null) return message.getId();
        return messageId;
    }

    public String getEffectiveSenderId() {
        if (message != null && message.getSenderId() != null) return message.getSenderId();
        return senderId;
    }

    public String getEffectiveReplyToMessageId() {
        if (message != null && message.getReplyToMessageId() != null) return message.getReplyToMessageId();
        return replyToMessageId;
    }

    public Double getEffectiveTimestamp() {
        if (message != null && message.getTimestamp() != null) return message.getTimestamp();
        return timestamp;
    }

    public Instant getEffectiveTimestampAsInstant() {
        Double ts = getEffectiveTimestamp();
        return ts != null ? Instant.ofEpochSecond(ts.longValue()) : null;
    }

    public String getEffectiveMessageType() {
        if (message != null && message.getMessageType() != null) return message.getMessageType();
        return messageType;
    }

    public Map<String, Object> getEffectivePayload() {
        if (message != null && message.getPayload() != null) return message.getPayload();
        return payload;
    }

    /**
     * Returns a one-line summary of all fields for logging/debugging.
     */
    public String toSummaryString() {
        return String.format(
            "type='%s', roomId='%s', senderId='%s', messageType='%s', payload=%s, timestamp=%s, messageId='%s', replyToMessageId='%s', projectId='%s'",
            String.valueOf(type),
            String.valueOf(roomId),
            String.valueOf(getEffectiveSenderId()),
            getEffectiveMessageType(),
            getEffectivePayload() != null ? getEffectivePayload().toString() : "null",
            String.valueOf(getEffectiveTimestamp()),
            String.valueOf(getEffectiveMessageId()),
            String.valueOf(getEffectiveReplyToMessageId()),
            String.valueOf(projectId)
        );
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "type='" + type + '\'' +
                ", roomId='" + roomId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", messageType='" + getEffectiveMessageType() + '\'' +
                ", payload=" + (getEffectivePayload() != null ? getEffectivePayload() : null) +
                ", timestamp=" + getEffectiveTimestamp() +
                ", messageId='" + getEffectiveMessageId() + '\'' +
                ", replyToMessageId='" + getEffectiveReplyToMessageId() + '\'' +
                ", projectId='" + projectId + '\'' +
                '}';
    }
}
