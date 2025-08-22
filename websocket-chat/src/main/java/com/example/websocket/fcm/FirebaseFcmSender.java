package com.example.websocket.fcm;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Firebase Admin SDK based implementation of {@link FcmSender}.
 *
 * Notes:
 * - Relies on Application Default Credentials in Cloud Run.
 * - Initializes a default FirebaseApp lazily if none exists.
 * - Sends data-first payloads; title/body optional for display.
 */
public final class FirebaseFcmSender implements FcmSender {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseFcmSender.class);
    private volatile boolean initialized = false;

    private void ensureInitialized() {
        if (initialized) return;
        synchronized (this) {
            if (initialized) return;
            if (FirebaseApp.getApps().isEmpty()) {
                try {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.getApplicationDefault())
                            .build();
                    FirebaseApp.initializeApp(options);
                    logger.info("🔥 Initialized FirebaseApp with ADC for Admin SDK");
                } catch (IOException e) {
                    logger.error("❌ Failed to initialize FirebaseApp with ADC: {}", e.getMessage());
                    throw new IllegalStateException("Failed to initialize FirebaseApp with ADC", e);
                }
            }
            initialized = true;
        }
    }

    @Override
    public SendResult sendToTokens(List<String> tokens, Map<String, String> data, String title, String body) {
        ensureInitialized();

        if (tokens == null || tokens.isEmpty()) {
            logger.debug("🔕 No tokens to send (data.type={}, data.channelId={})", data.get("type"), data.get("channelId"));
            return SendResult.builder().build();
        }

        Notification notification = null;
        if (title != null || body != null) {
            notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();
        }

        String type = data != null ? data.get("type") : null;
        String channelId = data != null ? data.get("channelId") : null;

        if (tokens.size() == 1) {
            String token = tokens.get(0);
            Message.Builder builder = Message.builder()
                    .putAllData(data)
                    .setToken(token);
            if (notification != null) builder.setNotification(notification);

            try {
                String messageId = FirebaseMessaging.getInstance().send(builder.build());
                logger.info("📤 FCM sent (single): type={}, channelId={}, msgId={}, tokenSuffix={}...",
                        type, channelId, messageId, safeSuffix(token));
                return SendResult.builder().addSuccess(messageId).build();
            } catch (Exception e) {
                String maybeInvalid = looksLikeInvalidToken(e) ? token : null;
                logger.warn("⚠️ FCM send failed (single): type={}, channelId={}, error={}, tokenSuffix={}...",
                        type, channelId, e.getMessage(), safeSuffix(token));
                return SendResult.builder().addFailure(maybeInvalid).build();
            }
        }

        MulticastMessage.Builder mmb = MulticastMessage.builder()
                .addAllTokens(tokens)
                .putAllData(data);
        if (notification != null) mmb.setNotification(notification);

        try {
            BatchResponse resp = FirebaseMessaging.getInstance().sendMulticast(mmb.build());
            SendResult.Builder result = SendResult.builder();
            List<SendResponse> responses = resp.getResponses();
            int success = 0, failure = 0;
            for (int i = 0; i < responses.size(); i++) {
                SendResponse r = responses.get(i);
                if (r.isSuccessful()) {
                    result.addSuccess(r.getMessageId());
                    success++;
                } else {
                    String t = tokens.get(i);
                    String maybeInvalid = r.getException() != null && looksLikeInvalidToken(r.getException()) ? t : null;
                    result.addFailure(maybeInvalid);
                    failure++;
                }
            }
            logger.info("📤 FCM sent (multicast): type={}, channelId={}, success={}, failure={} (targets={})",
                    type, channelId, success, failure, tokens.size());
            return result.build();
        } catch (Exception e) {
            logger.error("💥 FCM sendMulticast failed: type={}, channelId={}, error={}, targets={}",
                    type, channelId, e.getMessage(), tokens.size());
            SendResult.Builder result = SendResult.builder();
            for (int i = 0; i < tokens.size(); i++) result.addFailure(null);
            return result.build();
        }
    }

    private boolean looksLikeInvalidToken(Throwable t) {
        if (t == null) return false;
        String msg = String.valueOf(t.getMessage());
        return msg.contains("registration-token-not-registered")
                || msg.contains("mismatchSenderId")
                || msg.contains("invalid-argument")
                || msg.contains("Invalid registration token")
                || msg.contains("Requested entity was not found");
    }

    private String safeSuffix(String token) {
        if (token == null || token.length() < 6) return "***";
        return token.substring(Math.max(0, token.length() - 6));
    }
}
