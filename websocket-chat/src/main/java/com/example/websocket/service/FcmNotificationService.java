package com.example.websocket.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.FirebaseMessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * 간결한 FCM 알림 서비스
 * Firestore에서 직접 토큰을 조회하고 FCM 발송을 처리합니다.
 * Repository 추상화 없이 직접적인 구현으로 복잡성을 최소화했습니다.
 */
public final class FcmNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FcmNotificationService.class);
    
    private final Firestore db;
    private final FirebaseMessaging fcm;
    private final String deepLinkUrlPrefix;
    private final int maxSnippetLength;

    public FcmNotificationService() {
        this.db = FirestoreClient.getFirestore();
        this.fcm = FirebaseMessaging.getInstance();
        this.deepLinkUrlPrefix = "app://channel";
        this.maxSnippetLength = 100;
    }

    public FcmNotificationService(String deepLinkUrlPrefix, int maxSnippetLength) {
        this.db = FirestoreClient.getFirestore();
        this.fcm = FirebaseMessaging.getInstance();
        this.deepLinkUrlPrefix = deepLinkUrlPrefix;
        this.maxSnippetLength = maxSnippetLength;
    }

    /**
     * 멘션 알림을 전송합니다.
     * 
     * @param channelType 채널 타입
     * @param channelId 채널 ID
     * @param messageId 메시지 ID
     * @param senderId 발송자 ID
     * @param senderName 발송자 이름
     * @param fullText 전체 메시지 텍스트
     * @param mentionedUserIds 멘션된 사용자 ID 목록
     */
    public void sendMentionNotifications(String channelType,
                                       String channelId,
                                       String messageId,
                                       String senderId,
                                       String senderName,
                                       String fullText,
                                       List<String> mentionedUserIds) {
        if (mentionedUserIds == null || mentionedUserIds.isEmpty()) {
            return;
        }

        // 중복 제거 및 발송자 제외
        Set<String> uniqueTargets = new HashSet<>(mentionedUserIds);
        uniqueTargets.remove(senderId);

        logger.info("🔔 Mention notify: channelType={}, channelId={}, messageId={}, targets={} (sender={})",
                channelType, channelId, messageId, uniqueTargets.size(), senderId);

        for (String targetUserId : uniqueTargets) {
            try {
                sendNotificationToUser(targetUserId, channelType, channelId, messageId, senderId, senderName, fullText);
            } catch (Exception e) {
                logger.warn("⚠️ Mention notify failed for user {}: {}", targetUserId, e.getMessage());
            }
        }
    }

    /**
     * 특정 사용자에게 알림을 전송합니다.
     */
    private void sendNotificationToUser(String userId, String channelType, String channelId, 
                                      String messageId, String senderId, String senderName, String fullText) 
          throws ExecutionException, InterruptedException, FirebaseMessagingException {
        
        // 1. Firestore에서 FCM 토큰 조회
        String fcmToken = getUserFcmToken(userId);
        if (fcmToken == null || fcmToken.isEmpty()) {
            logger.debug("🔕 No FCM token for user {} — skipping mention", userId);
            return;
        }

        // 2. 메시지 데이터 구성
        String snippet = truncateText(fullText, maxSnippetLength);
        Map<String, String> data = buildNotificationData(channelType, channelId, messageId, senderId, senderName, snippet, userId);
        
        String title = buildNotificationTitle(senderName);
        String body = buildNotificationBody(snippet);

        // 3. FCM 메시지 생성 및 발송
        Message message = Message.builder()
                .setToken(fcmToken)
                .putAllData(data)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            String response = fcm.send(message);
            logger.info("📊 Mention FCM success: userId={}, response={}", userId, response);
        } catch (FirebaseMessagingException e) {
            if (isInvalidTokenError(e)) {
                // 무효한 토큰인 경우 Firestore에서 제거
                removeInvalidToken(userId, fcmToken);
                logger.info("🧹 Removed invalid FCM token for user {}", userId);
            }
            throw e;
        }
    }

    /**
     * Firestore에서 사용자의 FCM 토큰을 조회합니다.
     */
    private String getUserFcmToken(String userId) throws ExecutionException, InterruptedException {
        DocumentReference userDoc = db.collection("users").document(userId);
        DocumentSnapshot snap = userDoc.get().get();
        
        if (snap.exists()) {
            String token = snap.getString("fcmToken");
            return (token != null && !token.trim().isEmpty()) ? token : null;
        }
        return null;
    }

    /**
     * 무효한 FCM 토큰을 Firestore에서 제거합니다.
     */
    private void removeInvalidToken(String userId, String invalidToken) {
        try {
            DocumentReference userDoc = db.collection("users").document(userId);
            DocumentSnapshot snap = userDoc.get().get();
            
            if (snap.exists()) {
                String currentToken = snap.getString("fcmToken");
                if (invalidToken.equals(currentToken)) {
                    ApiFuture<WriteResult> future = userDoc.update("fcmToken", null);
                    future.get();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to remove invalid FCM token for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 알림 데이터 맵을 구성합니다.
     */
    private Map<String, String> buildNotificationData(String channelType, String channelId, String messageId, 
                                                     String senderId, String senderName, String snippet, String targetUserId) {
        return Map.of(
                "type", "mention",
                "messageId", messageId,
                "channelId", channelId,
                "channelType", channelType,
                "senderId", senderId,
                "senderName", senderName,
                "snippet", snippet,
                "deepLink", deepLinkUrlPrefix + "/" + channelId,
                "mentionType", "USER",
                "mentionId", targetUserId
        );
    }

    /**
     * 알림 제목을 생성합니다.
     */
    private String buildNotificationTitle(String senderName) {
        return senderName + "님이 회원님을 멘션했습니다";
    }

    /**
     * 알림 본문을 생성합니다.
     */
    private String buildNotificationBody(String snippet) {
        return snippet.isEmpty() ? "새로운 멘션이 있습니다" : snippet;
    }

    /**
     * 텍스트를 최대 길이로 자릅니다.
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        text = text.trim();
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * FCM 오류가 무효한 토큰 오류인지 확인합니다.
     */
    private boolean isInvalidTokenError(FirebaseMessagingException e) {
        String errorCode = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "";
        return "INVALID_ARGUMENT".equals(errorCode) || 
               "UNREGISTERED".equals(errorCode) || 
               e.getMessage().contains("registration-token-not-registered");
    }
}