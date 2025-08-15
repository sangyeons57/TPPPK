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
                } catch (IOException e) {
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
            return SendResult.builder().build();
        }

        Notification notification = null;
        if (title != null || body != null) {
            notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();
        }

        if (tokens.size() == 1) {
            String token = tokens.get(0);
            Message.Builder builder = Message.builder()
                    .putAllData(data)
                    .setToken(token);
            if (notification != null) builder.setNotification(notification);

            try {
                String messageId = FirebaseMessaging.getInstance().send(builder.build());
                return SendResult.builder().addSuccess(messageId).build();
            } catch (Exception e) {
                // Classify invalid-token-like errors heuristically by message text
                String maybeInvalid = looksLikeInvalidToken(e) ? token : null;
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
            for (int i = 0; i < responses.size(); i++) {
                SendResponse r = responses.get(i);
                if (r.isSuccessful()) {
                    result.addSuccess(r.getMessageId());
                } else {
                    String t = tokens.get(i);
                    String maybeInvalid = r.getException() != null && looksLikeInvalidToken(r.getException()) ? t : null;
                    result.addFailure(maybeInvalid);
                }
            }
            return result.build();
        } catch (Exception e) {
            // On total failure, mark all as failed without invalid token classification
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
}
