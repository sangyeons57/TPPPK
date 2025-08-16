package com.example.websocket.model;

import com.example.websocket.constants.WebSocketEventConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Nested domain message wrapper for WebSocket transport (legacy/compat only).
 *
 * Mapping to domain Message (server reads these first, then falls back to envelope):
 * - id                → Message.id
 * - messageType       → Message.messageType (string ↔ enum handled elsewhere)
 * - payload           → Message.payload (full JSON)
 * - senderId          → Message.senderId
 * - replyToMessageId  → Message.replyToMessageId
 * - timestamp         → Message.createdAt (epoch seconds → Instant)
 *
 * Note: Newer clients are encouraged to send flat (envelope-only) fields; server will still
 * parse this nested object if provided for backward compatibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageData {

    @JsonProperty(WebSocketEventConstants.FIELD_MESSAGE_ID)
    private String id;

    @JsonProperty(WebSocketEventConstants.FIELD_MESSAGE_TYPE)
    private String messageType;

    @JsonProperty(WebSocketEventConstants.FIELD_PAYLOAD)
    private Map<String, Object> payload;

    @JsonProperty(WebSocketEventConstants.FIELD_SENDER_ID)
    private String senderId;

    @JsonProperty(WebSocketEventConstants.FIELD_REPLY_TO_MESSAGE_ID)
    private String replyToMessageId;

    @JsonProperty(WebSocketEventConstants.FIELD_TIMESTAMP)
    private Double timestamp;

    public MessageData() {}

    public MessageData(String id, String messageType, Map<String, Object> payload, String senderId, String replyToMessageId, Double timestamp) {
        this.id = id;
        this.messageType = messageType;
        this.payload = payload;
        this.senderId = senderId;
        this.replyToMessageId = replyToMessageId;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public Double getTimestamp() { return timestamp; }
    public void setTimestamp(Double timestamp) { this.timestamp = timestamp; }
}
