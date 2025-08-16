package com.example.websocket.service;

import com.example.websocket.config.FirebaseConfig;
import com.example.websocket.constants.FirestoreConstants;
import com.example.websocket.constants.PayloadConstants;
import com.example.websocket.constants.WebSocketEventConstants;
import com.example.websocket.model.ChatMessage;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Firestore에 채팅 메시지를 저장하는 서비스
 */
public class FirestoreMessageService {
    private static final Logger logger = LoggerFactory.getLogger(FirestoreMessageService.class);
    
    private final Firestore firestore;
    private final ObjectMapper objectMapper;
    
    public FirestoreMessageService() {
        if (FirebaseConfig.isInitialized()) {
            this.firestore = FirestoreClient.getFirestore();
            logger.info("✅ FirestoreMessageService initialized with Firebase {}", firestore);
        } else {
            this.firestore = null;
            logger.warn("❌ FirestoreMessageService initialized without Firebase (mock mode)");
        }
        
        // ObjectMapper 설정 (표준 JSON 생성 보장)
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        logger.debug("✅ ObjectMapper initialized for standard JSON serialization");
    }
    
    /**
     * 채팅 메시지를 Firestore에 저장
     *
     * 매핑 규칙(중요): ChatMessage → Firestore 문서
     * - senderId              ← message.message.senderId 우선, 없으면 message.senderId
     * - messageType           ← message.message.messageType 우선, 없으면 message.messageType 또는 TEXT
     * - payload (string JSON) ← message.message.payload 우선, 없으면 message.payload (전체 JSON 문자열로 직렬화)
     * - channelId             ← roomId 인자(봉투)
     * - createdAt, updatedAt  ← message.message.timestamp 또는 message.timestamp (epoch seconds)
     * - isDeleted             ← 기본 false (삭제 API에서 true)
     * - replyToMessageId      ← message.message.replyToMessageId 우선, 없으면 message.replyToMessageId
     * - mentions              ← 빈 배열 초기화 (향후 필요 시 파싱)
     *
     * @param roomId 채팅방 ID (Firestore 문서의 channelId)
     * @param message 저장할 메시지 DTO (nested 우선, envelope 폴백)
     * @return 저장 결과 CompletableFuture
     */
    public CompletableFuture<Boolean> saveMessage(String roomId, ChatMessage message) {
        if (firestore == null) {
            logger.warn("⚠️ Firestore not available, skipping message save: {}", message.getMessageId());
            return CompletableFuture.completedFuture(false);
        }
        
        try {
            String collectionPath;
            String logInfo;
            
            logger.info("💾 Processing message: roomId={}, projectId={}", 
                       roomId, message.getProjectId());
            
            // 채널 유형은 projectId 존재 여부로 결정
            if (message.getProjectId() != null && !message.getProjectId().trim().isEmpty()) {
                collectionPath = FirestoreConstants.COLLECTION_PROJECTS + "/" + message.getProjectId() + "/" + FirestoreConstants.COLLECTION_CHANNELS + "/" + roomId + "/" + FirestoreConstants.COLLECTION_MESSAGES;
                logInfo = "projectId=" + message.getProjectId() + ", channelId=" + roomId;
                logger.info("💾 Project channel detected: projectId={}, channelId={}", message.getProjectId(), roomId);
            } else {
                collectionPath = "dm_channels/" + roomId + "/" + FirestoreConstants.COLLECTION_MESSAGES;
                logInfo = "dmChannelId=" + roomId;
                logger.info("💾 DM channel detected: channelId={}", roomId);
            }


            // 메시지 데이터 구성 (payload + messageType 사용)
            // 도메인 소유 필드는 nested(message.*) 우선, 없으면 봉투(envelope)에서 폴백
            Map<String, Object> messageData = new HashMap<>();
            String effectiveSenderId = (message.getMessage() != null && message.getMessage().getSenderId() != null)
                    ? message.getMessage().getSenderId() : message.getSenderId();
            String effectiveMessageId = (message.getMessage() != null && message.getMessage().getId() != null)
                    ? message.getMessage().getId() : message.getMessageId();
            String effectiveReplyTo = (message.getMessage() != null && message.getMessage().getReplyToMessageId() != null)
                    ? message.getMessage().getReplyToMessageId() : message.getReplyToMessageId();
            java.time.Instant effectiveCreatedAt = message.getEffectiveTimestampAsInstant();

            messageData.put(FirestoreConstants.FIELD_SENDER_ID, effectiveSenderId);
            // Prefer nested message.messageType if present
            String effectiveMessageType = (message.getMessage() != null && message.getMessage().getMessageType() != null)
                    ? message.getMessage().getMessageType()
                    : (message.getMessageType() != null ? message.getMessageType() : WebSocketEventConstants.MESSAGE_TYPE_TEXT);
            messageData.put(FirestoreConstants.FIELD_MESSAGE_TYPE, effectiveMessageType);
            
            // payload 전용 처리 (전체 JSON 원형 보존)
            String payloadJson;
            Map<String, Object> effectivePayload = (message.getMessage() != null && message.getMessage().getPayload() != null)
                    ? message.getMessage().getPayload()
                    : message.getPayload();
            if (effectivePayload != null && !effectivePayload.isEmpty()) {
                payloadJson = convertMapToJson(effectivePayload);
            } else {
                // 빈 payload
                payloadJson = "{}";
            }
            messageData.put(FirestoreConstants.FIELD_PAYLOAD, payloadJson);
            
            messageData.put(FirestoreConstants.FIELD_CHANNEL_ID, roomId); // ✅ channelId 필드 추가
            messageData.put(FirestoreConstants.FIELD_CREATED_AT, effectiveCreatedAt);
            messageData.put(FirestoreConstants.FIELD_UPDATED_AT, effectiveCreatedAt);
            messageData.put(FirestoreConstants.FIELD_IS_DELETED, false);
            messageData.put(FirestoreConstants.FIELD_REPLY_TO_MESSAGE_ID, effectiveReplyTo);
            messageData.put(FirestoreConstants.FIELD_MENTIONS, new java.util.ArrayList<>()); // 빈 배열로 초기화
            
            logger.info("💾 Saving message to Firestore: {}, messageId(effective)={}, senderId={}", 
                       logInfo, effectiveMessageId, effectiveSenderId);

            logger.info("💾 Collection Path detected: collectionPath={}, messageId(effective)={}, messageData={}", collectionPath, effectiveMessageId, messageData);

            // Ensure non-empty document id; auto-generate if missing
            DocumentReference docRef;
            if (effectiveMessageId == null || effectiveMessageId.trim().isEmpty()) {
                docRef = firestore.collection(collectionPath).document();
                effectiveMessageId = docRef.getId();
                logger.warn("⚠️ Missing messageId; auto-generated Firestore doc id: {}", effectiveMessageId);
            } else {
                docRef = firestore.collection(collectionPath).document(effectiveMessageId);
            }

            ApiFuture<WriteResult> future = docRef.set(messageData);

            return toCompletableFuture(future)
                .thenApply(result -> {
                    logger.info("✅ Message saved to Firestore successfully: {}", message.getMessageId());
                    return true;
                })
                .exceptionally(throwable -> {
                    logger.error("❌ Failed to save message to Firestore: {} - {}", 
                               message.getMessageId(), throwable.getMessage());
                    return false;
                });
                
        } catch (Exception e) {
            logger.error("❌ Error preparing message for Firestore save: {} - {}", 
                       message.getMessageId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 메시지 편집을 Firestore에 반영 (upsert 방식으로 createdAt 원본 시간 유지)
     */
    public CompletableFuture<Boolean> updateMessage(String roomId, ChatMessage message) {
        if (firestore == null) {
            logger.warn("⚠️ Firestore not available, skipping message update: {}", message.getMessageId());
            return CompletableFuture.completedFuture(false);
        }
        
        try {
            String collectionPath;
            String logInfo;
            
            // 채널 유형은 projectId 존재 여부로 결정
            if (message.getProjectId() != null && !message.getProjectId().trim().isEmpty()) {
                collectionPath = "projects/" + message.getProjectId() + "/channels/" + roomId + "/messages";
                logInfo = "projectId=" + message.getProjectId() + ", channelId=" + roomId;
            } else {
                collectionPath = "dm_channels/" + roomId + "/messages";
                logInfo = "dmChannelId=" + roomId;
            }
            
            DocumentReference docRef = firestore.collection(collectionPath).document(message.getMessageId());
            
            // 먼저 기존 문서 조회하여 createdAt 확인
            return toCompletableFuture(docRef.get())
                .thenCompose(documentSnapshot -> {
                    Map<String, Object> upsertData = new HashMap<>();
                    
                    // 기본 필드들
                    String effectiveSenderId = (message.getMessage() != null && message.getMessage().getSenderId() != null)
                            ? message.getMessage().getSenderId() : message.getSenderId();
                    upsertData.put(FirestoreConstants.FIELD_SENDER_ID, effectiveSenderId);
                    // Prefer nested message.messageType if present
                    String effectiveMessageType = (message.getMessage() != null && message.getMessage().getMessageType() != null)
                            ? message.getMessage().getMessageType()
                            : (message.getMessageType() != null ? message.getMessageType() : WebSocketEventConstants.MESSAGE_TYPE_TEXT);
                    upsertData.put(FirestoreConstants.FIELD_MESSAGE_TYPE, effectiveMessageType);
                    upsertData.put(FirestoreConstants.FIELD_CHANNEL_ID, roomId);
                    String effectiveReplyTo = (message.getMessage() != null && message.getMessage().getReplyToMessageId() != null)
                            ? message.getMessage().getReplyToMessageId() : message.getReplyToMessageId();
                    upsertData.put(FirestoreConstants.FIELD_REPLY_TO_MESSAGE_ID, effectiveReplyTo);
                    upsertData.put(FirestoreConstants.FIELD_IS_DELETED, false);
                    
                    // payload 처리
                    String payloadJson;
                    Map<String, Object> effectivePayload = (message.getMessage() != null && message.getMessage().getPayload() != null)
                            ? message.getMessage().getPayload()
                            : message.getPayload();
                    if (effectivePayload != null && !effectivePayload.isEmpty()) {
                        payloadJson = convertMapToJson(effectivePayload);
                    } else {
                        payloadJson = "{}";
                    }
                    upsertData.put(FirestoreConstants.FIELD_PAYLOAD, payloadJson);
                    
                    // createdAt 처리: 기존 문서가 있으면 유지, 없으면 현재 시간
                    if (documentSnapshot.exists() && documentSnapshot.contains(FirestoreConstants.FIELD_CREATED_AT)) {
                        // 기존 createdAt 유지
                        Object existingCreatedAt = documentSnapshot.get(FirestoreConstants.FIELD_CREATED_AT);
                        upsertData.put(FirestoreConstants.FIELD_CREATED_AT, existingCreatedAt);
                        logger.info("✏️ Updating existing message, preserving createdAt: {}", message.getMessageId());
                    } else {
                        // 새 문서 생성 시 현재 시간으로 createdAt 설정
                        upsertData.put(FirestoreConstants.FIELD_CREATED_AT, message.getEffectiveTimestampAsInstant());
                        upsertData.put(FirestoreConstants.FIELD_MENTIONS, new java.util.ArrayList<>());
                        logger.info("✏️ Creating new message during edit operation: {}", message.getMessageId());
                    }
                    
                    // updatedAt는 항상 현재 시간
                    upsertData.put(FirestoreConstants.FIELD_UPDATED_AT, message.getEffectiveTimestampAsInstant());
                    
                    logger.info("✏️ Upserting message in Firestore: {}, messageId={}", 
                               logInfo, message.getMessageId());
                    
                    // SetOptions.merge()를 사용하여 upsert 수행
                    ApiFuture<WriteResult> future = docRef.set(upsertData, SetOptions.merge());
                    return toCompletableFuture(future);
                })
                .thenApply(result -> {
                    logger.info("✅ Message upserted in Firestore successfully: {}", message.getMessageId());
                    return true;
                })
                .exceptionally(throwable -> {
                    logger.error("❌ Failed to upsert message in Firestore: {} - {}", 
                               message.getMessageId(), throwable.getMessage());
                    return false;
                });
                
        } catch (Exception e) {
            logger.error("❌ Error preparing message for Firestore upsert: {} - {}", 
                       message.getMessageId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 메시지 삭제를 Firestore에 반영
     */
    public CompletableFuture<Boolean> deleteMessage(String roomId, ChatMessage message) {
        if (firestore == null) {
            logger.warn("⚠️ Firestore not available, skipping message delete: {}", message.getMessageId());
            return CompletableFuture.completedFuture(false);
        }
        
        try {
            String collectionPath;
            String logInfo;
            
            // 채널 유형은 projectId 존재 여부로 결정
            if (message.getProjectId() != null && !message.getProjectId().trim().isEmpty()) {
                collectionPath = "projects/" + message.getProjectId() + "/channels/" + roomId + "/messages";
                logInfo = "projectId=" + message.getProjectId() + ", channelId=" + roomId;
            } else {
                collectionPath = "dm_channels/" + roomId + "/messages";
                logInfo = "dmChannelId=" + roomId;
            }
            
            DocumentReference docRef = firestore.collection(collectionPath).document(message.getMessageId());
            
            Map<String, Object> deleteData = new HashMap<>();
            deleteData.put(FirestoreConstants.FIELD_IS_DELETED, true);
            deleteData.put(FirestoreConstants.FIELD_UPDATED_AT, message.getTimestampAsInstant());
            
            logger.info("🗑️ Marking message as deleted in Firestore: {}, messageId={}", 
                       logInfo, message.getMessageId());
            
            ApiFuture<WriteResult> future = docRef.update(deleteData);
            return toCompletableFuture(future)
                .thenApply(result -> {
                    logger.info("✅ Message marked as deleted in Firestore successfully: {}", message.getMessageId());
                    return true;
                })
                .exceptionally(throwable -> {
                    logger.error("❌ Failed to delete message in Firestore: {} - {}", 
                               message.getMessageId(), throwable.getMessage());
                    return false;
                });
                
        } catch (Exception e) {
            logger.error("❌ Error preparing message for Firestore delete: {} - {}", 
                       message.getMessageId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * ApiFuture를 CompletableFuture로 변환하는 헬퍼 메서드
     */
    private <T> CompletableFuture<T> toCompletableFuture(ApiFuture<T> apiFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        
        ApiFutures.addCallback(apiFuture, new ApiFutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                completableFuture.complete(result);
            }
            
            @Override
            public void onFailure(Throwable throwable) {
                completableFuture.completeExceptionally(throwable);
            }
        }, MoreExecutors.directExecutor());
        
        return completableFuture;
    }
    
    
    /**
     * Map을 표준 JSON 문자열로 변환하는 헬퍼 메서드 (Jackson ObjectMapper 사용)
     * 기존 수동 문자열 연결 방식에서 Jackson으로 전환하여 표준 JSON 보장
     */
    private String convertMapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        try {
            String jsonResult = objectMapper.writeValueAsString(map);
            logger.debug("✅ Map을 표준 JSON로 변환 성공: {}", jsonResult);
            return jsonResult;
        } catch (Exception e) {
            logger.warn("⚠️ Map을 JSON으로 변환 실패, 빈 객체 반환: {}", e.getMessage());
            return "{}";
        }
    }
}
