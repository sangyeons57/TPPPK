package com.example.websocket.model;

import com.example.websocket.constants.WebSocketEventConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    // ================================
    // 페이로드 키 상수 집합
    // ================================
    
    /**
     * WebSocket 메시지 페이로드에서 사용되는 실제 키들을 Android 클라이언트와 일치하게 관리.
     * MessagePayload.kt 및 실제 사용 패턴을 바탕으로 정의.
     */
    public static final class PayloadKeys {
        
        // ================================
        // Core Content Keys
        // ================================
        
        /** 메인 텍스트 내용 */
        public static final String CONTENT = "content";
        
        /** 첨부파일 배열 */
        public static final String ATTACHMENTS = "attachments";
        
        // ================================
        // Attachment Keys (attachments 배열 내 객체에서 사용)
        // ================================
        
        /** 첨부파일 URL/URI */
        public static final String URL = "url";
        
        /** 원본 파일명 */
        public static final String FILENAME = "filename";
        
        /** MIME 타입 */
        public static final String MIME = "mime";
        
        /** 파일 확장자 */
        public static final String EXT = "ext";
        
        /** 첨부파일 순서 */
        public static final String INDEX = "index";
        
        // ================================
        // Upload State Keys
        // ================================
        
        /** 업로드 진행률 (0.0-1.0) */
        public static final String UPLOAD_PROGRESS = "uploadProgress";
        
        /** 업로드 중 표시 (boolean) */
        public static final String UPLOADING = "uploading";
        
        // ================================
        // Optimistic Update Keys
        // ================================
        
        /** 낙관적 업데이트 메타데이터 컨테이너 */
        public static final String META = "_meta";
        
        /** 대기 중인 작업 ("edit", "delete") */
        public static final String PENDING_OP = "pendingOp";
        
        /** 롤백용 백업 페이로드 */
        public static final String BACKUP_PAYLOAD = "backupPayload";
        
        // ================================
        // System Message Keys
        // ================================
        
        /** 시스템 메시지 타입 */
        public static final String SYSTEM_TYPE = "systemType";
        
        /** 날짜 문자열 (SYSTEM_DATE용) */
        public static final String DATE = "date";
        
        /** 표시용 텍스트 */
        public static final String DISPLAY_TEXT = "displayText";
        
        /** 채널명 (CHAT_START용) */
        public static final String CHANNEL_NAME = "channelName";
        
        /** 환영 메시지 */
        public static final String WELCOME_TEXT = "welcomeText";
        
        // ================================
        // Project Message Keys (PROJECT_INVITE & SYSTEM_PROJECT_*)
        // ================================
        
        /** 프로젝트 식별자 */
        public static final String PROJECT_ID = "projectId";
        
        /** 프로젝트 표시명 */
        public static final String PROJECT_NAME = "projectName";
        
        /** 초대자 이름 */
        public static final String INVITER_NAME = "inviterName";
        
        /** 초대 식별자 (PROJECT_INVITE에만 사용) */
        public static final String INVITATION_ID = "invitationId";
        
        /** 대상 사용자 (MEMBER_INVITATION용) */
        public static final String TARGET_USER_ID = "targetUserId";
        
        /** 버튼 텍스트 (기본값: "참여하기") */
        public static final String ACTION_TEXT = "actionText";
        
        // ================================
        // Mentions (Message level, not payload)
        // ================================
        
        /** 멘션 대상 목록 */
        public static final String MENTIONS = "mentions";
        
        // Private constructor to prevent instantiation
        private PayloadKeys() {
            throw new AssertionError("Cannot instantiate utility class");
        }
    }
    /**
     * 도메인 Message 전송을 위한 순수 데이터 객체 (NestedMessage 역할).
     * WebSocket 통신 필드는 WebSocketMessage에서 처리하고, 이 클래스는 순수한 도메인 데이터만 포함.
     *
     * 도메인 Message ← ChatMessage 매핑 규칙:
     * - Message.id               ← id
     * - Message.senderId         ← senderId  
     * - Message.messageType      ← messageType
     * - Message.payload          ← payload
     * - Message.replyToMessageId ← replyToMessageId
     * - Message.createdAt        ← timestamp (epoch seconds)
     */
    

    // 메시지 데이터 필드들 (MessageData에서 통합)
    @JsonProperty(WebSocketEventConstants.FIELD_MESSAGE_ID)
    private String id;
    
    @JsonProperty(WebSocketEventConstants.FIELD_MESSAGE_TYPE)
    private String messageType = WebSocketEventConstants.MESSAGE_TYPE_TEXT; // Default to TEXT type
    
    @JsonProperty(WebSocketEventConstants.FIELD_PAYLOAD)
    private Map<String, Object> payload;
    
    @JsonProperty(WebSocketEventConstants.FIELD_SENDER_ID)
    private String senderId;
    
    @JsonProperty(WebSocketEventConstants.FIELD_REPLY_TO_MESSAGE_ID)
    private String replyToMessageId;
    
    @JsonProperty(WebSocketEventConstants.FIELD_TIMESTAMP)
    private Double timestamp; // Changed to Double to match client

    // Message-level mentions (not part of payload). Optional.
    @JsonProperty(WebSocketEventConstants.FIELD_MENTIONS_ID)
    private List<MentionItem> mentions;

    public ChatMessage() {}

    public ChatMessage(String senderId, Map<String, Object> payload, Instant timestamp) {
        this.senderId = senderId;
        this.payload = payload;
        this.setTimestampFromInstant(timestamp);
    }

    // Factory methods for payload-based messages
    public static ChatMessage createWithTextPayload(String senderId, String messageType, String textContent, Instant timestamp) {
        ChatMessage message = new ChatMessage();
        message.senderId = senderId;
        message.messageType = messageType != null ? messageType : WebSocketEventConstants.MESSAGE_TYPE_TEXT;
        
        // Create payload
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put(PayloadKeys.CONTENT, textContent != null ? textContent : "");
        message.payload = payloadMap;
        message.setTimestampFromInstant(timestamp);
        
        // Generate ID if not provided
        if (message.id == null || message.id.trim().isEmpty()) {
            message.id = java.util.UUID.randomUUID().toString();
        }
        
        return message;
    }
    
    public static ChatMessage createSystemMessage(String senderId, String systemText, Instant timestamp) {
        return createWithTextPayload(senderId, WebSocketEventConstants.MESSAGE_TYPE_SYSTEM, systemText, timestamp);
    }

    // Getters and Setters

    // 메시지 데이터 getters/setters
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

    public List<MentionItem> getMentions() { return mentions; }
    public void setMentions(List<MentionItem> mentions) { this.mentions = mentions; }
    
    // Convenience method to get timestamp as Instant
    public Instant getTimestampAsInstant() {
        return timestamp != null ? Instant.ofEpochSecond(timestamp.longValue()) : null;
    }
    
    // Convenience method to set timestamp from Instant
    public void setTimestampFromInstant(Instant instant) {
        this.timestamp = instant != null ? (double) instant.getEpochSecond() : null;
    }


    /**
     * Returns a one-line summary of all fields for logging/debugging.
     */
    public String toSummaryString() {
        return String.format(
            "senderId='%s', messageType='%s', payload=%s, timestamp=%s, messageId='%s', replyToMessageId='%s'",
            String.valueOf(senderId),
            messageType,
            payload != null ? payload.toString() : "null",
            String.valueOf(timestamp),
            String.valueOf(id),
            String.valueOf(replyToMessageId)
        );
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "senderId='" + senderId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", payload=" + (payload != null ? payload : null) +
                ", timestamp=" + timestamp +
                ", messageId='" + id + '\'' +
                ", replyToMessageId='" + replyToMessageId + '\'' +
                ", mentions=" + (mentions != null ? mentions.size() : 0) +
                '}';
    }
}
