package com.example.websocket.service;

import com.example.websocket.config.FirebaseConfig;
import com.example.websocket.model.ChatMessage;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Firestore에 채팅 메시지를 저장하는 서비스
 */
public class FirestoreMessageService {
    private static final Logger logger = LoggerFactory.getLogger(FirestoreMessageService.class);
    
    private final Firestore firestore;
    
    public FirestoreMessageService() {
        if (FirebaseConfig.isInitialized()) {
            this.firestore = FirestoreClient.getFirestore();
            logger.info("✅ FirestoreMessageService initialized with Firebase {}", firestore);
        } else {
            this.firestore = null;
            logger.warn("❌ FirestoreMessageService initialized without Firebase (mock mode)");
        }
    }
    
    /**
     * 채팅 메시지를 Firestore에 저장
     * @param roomId 채팅방 ID
     * @param message 저장할 메시지
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
            
            logger.info("💾 Processing message: roomId={}, channelType={}, projectId={}", 
                       roomId, message.getChannelType(), message.getProjectId());
            
            // channelType을 기반으로 Firestore 경로 결정
            if ("DM".equals(message.getChannelType())) {
                // DM 채널: dm_channels/{channelId}/messages
                collectionPath = "dm_channels/" + roomId + "/messages";
                logInfo = "dmChannelId=" + roomId;
                logger.info("💾 DM channel detected: channelId={}", roomId);
            } else if ("PROJECT".equals(message.getChannelType())) {
                // 프로젝트 채널: projects/{projectId}/channels/{channelId}/messages
                if (message.getProjectId() == null || message.getProjectId().trim().isEmpty()) {
                    logger.error("❌ ProjectId is required for PROJECT channel type. roomId={}", roomId);
                    return CompletableFuture.completedFuture(false);
                }
                collectionPath = "projects/" + message.getProjectId() + "/channels/" + roomId + "/messages";
                logInfo = "projectId=" + message.getProjectId() + ", channelId=" + roomId;
                logger.info("💾 Project channel detected: projectId={}, channelId={}", message.getProjectId(), roomId);
            } else {
                logger.error("❌ Unknown channelType: {}. Expected 'DM' or 'PROJECT'", message.getChannelType());
                return CompletableFuture.completedFuture(false);
            }


            // 메시지 데이터 구성 (payload + messageType 사용)
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("senderId", message.getSenderId());
            messageData.put("messageType", message.getMessageType() != null ? message.getMessageType() : "TEXT");
            
            // payload 전용 처리 (일관성을 위해 content는 deprecated)
            String payloadJson;
            if (message.getPayload() != null && !message.getPayload().isEmpty()) {
                // payload 우선 사용
                payloadJson = convertMapToJson(message.getPayload());
            } else if (message.getContent() != null && !message.getContent().isEmpty()) { 
                // 백워드 호환성: content를 TEXT payload로 변환
                Map<String, Object> textPayload = new HashMap<>();
                textPayload.put("content", message.getContent());
                payloadJson = convertMapToJson(textPayload);
                logger.warn("⚠️ Using deprecated content field, converting to payload: {}", message.getMessageId());
            } else {
                // 빈 payload
                payloadJson = "{}";
            }
            messageData.put("payload", payloadJson);
            
            messageData.put("channelId", roomId); // ✅ channelId 필드 추가
            messageData.put("createdAt", message.getTimestampAsInstant());
            messageData.put("updatedAt", message.getTimestampAsInstant());
            messageData.put("isDeleted", false);
            messageData.put("replyToMessageId", message.getReplyToMessageId());
            messageData.put("mentions", new java.util.ArrayList<>()); // 빈 배열로 초기화
            
            logger.info("💾 Saving message to Firestore: {}, messageId={}, senderId={}", 
                       logInfo, message.getMessageId(), message.getSenderId());

            logger.info("💾 Collection Path detected: collectionPath={}, messageId={}, messageDta={}", collectionPath, message.getMessageId(), messageData);
            ApiFuture<WriteResult> future = firestore.collection(collectionPath).document(message.getMessageId()).set(messageData);

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
     * 메시지 편집을 Firestore에 반영
     */
    public CompletableFuture<Boolean> updateMessage(String roomId, ChatMessage message) {
        if (firestore == null) {
            logger.warn("⚠️ Firestore not available, skipping message update: {}", message.getMessageId());
            return CompletableFuture.completedFuture(false);
        }
        
        try {
            String collectionPath;
            String logInfo;
            
            // channelType을 기반으로 Firestore 경로 결정
            if ("DM".equals(message.getChannelType())) {
                collectionPath = "dm_channels/" + roomId + "/messages";
                logInfo = "dmChannelId=" + roomId;
            } else if ("PROJECT".equals(message.getChannelType())) {
                if (message.getProjectId() == null || message.getProjectId().trim().isEmpty()) {
                    logger.error("❌ ProjectId is required for PROJECT channel type update. roomId={}", roomId);
                    return CompletableFuture.completedFuture(false);
                }
                collectionPath = "projects/" + message.getProjectId() + "/channels/" + roomId + "/messages";
                logInfo = "projectId=" + message.getProjectId() + ", channelId=" + roomId;
            } else {
                logger.error("❌ Unknown channelType for update: {}. Expected 'DM' or 'PROJECT'", message.getChannelType());
                return CompletableFuture.completedFuture(false);
            }
            
            DocumentReference docRef = firestore.collection(collectionPath).document(message.getMessageId());
            
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("messageType", message.getMessageType() != null ? message.getMessageType() : "TEXT");
            
            // payload 전용 업데이트
            String payloadJson;
            if (message.getPayload() != null && !message.getPayload().isEmpty()) {
                // payload 우선 사용
                payloadJson = convertMapToJson(message.getPayload());
            } else if (message.getContent() != null && !message.getContent().isEmpty()) {
                // 백워드 호환성: content를 TEXT payload로 변환
                Map<String, Object> textPayload = new HashMap<>();
                textPayload.put("content", message.getContent());
                payloadJson = convertMapToJson(textPayload);
                logger.warn("⚠️ Using deprecated content field for update, converting to payload: {}", message.getMessageId());
            } else {
                // 빈 payload
                payloadJson = "{}";
            }
            updateData.put("payload", payloadJson);
            updateData.put("updatedAt", message.getTimestampAsInstant());
            
            logger.info("✏️ Updating message in Firestore: {}, messageId={}", 
                       logInfo, message.getMessageId());
            
            ApiFuture<WriteResult> future = docRef.update(updateData);
            return toCompletableFuture(future)
                .thenApply(result -> {
                    logger.info("✅ Message updated in Firestore successfully: {}", message.getMessageId());
                    return true;
                })
                .exceptionally(throwable -> {
                    logger.error("❌ Failed to update message in Firestore: {} - {}", 
                               message.getMessageId(), throwable.getMessage());
                    return false;
                });
                
        } catch (Exception e) {
            logger.error("❌ Error preparing message for Firestore update: {} - {}", 
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
            
            // channelType을 기반으로 Firestore 경로 결정
            if ("DM".equals(message.getChannelType())) {
                collectionPath = "dm_channels/" + roomId + "/messages";
                logInfo = "dmChannelId=" + roomId;
            } else if ("PROJECT".equals(message.getChannelType())) {
                if (message.getProjectId() == null || message.getProjectId().trim().isEmpty()) {
                    logger.error("❌ ProjectId is required for PROJECT channel type delete. roomId={}", roomId);
                    return CompletableFuture.completedFuture(false);
                }
                collectionPath = "projects/" + message.getProjectId() + "/channels/" + roomId + "/messages";
                logInfo = "projectId=" + message.getProjectId() + ", channelId=" + roomId;
            } else {
                logger.error("❌ Unknown channelType for delete: {}. Expected 'DM' or 'PROJECT'", message.getChannelType());
                return CompletableFuture.completedFuture(false);
            }
            
            DocumentReference docRef = firestore.collection(collectionPath).document(message.getMessageId());
            
            Map<String, Object> deleteData = new HashMap<>();
            deleteData.put("isDeleted", true);
            deleteData.put("updatedAt", message.getTimestampAsInstant());
            
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
     * Map을 간단한 JSON 문자열로 변환하는 헬퍼 메서드 (String values)
     */
    private String convertStringMapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            json.append("\"").append(entry.getValue() != null ? entry.getValue() : "").append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }
    
    /**
     * Map을 간단한 JSON 문자열로 변환하는 헬퍼 메서드 (Object values)
     */
    private String convertMapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                json.append("\"").append(entry.getValue()).append("\"");
            } else {
                json.append(entry.getValue());
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }
}