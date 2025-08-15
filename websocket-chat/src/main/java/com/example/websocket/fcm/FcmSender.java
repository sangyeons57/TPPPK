package com.example.websocket.fcm;

import java.util.List;
import java.util.Map;

/**
 * Simple abstraction over Firebase Messaging for sending push notifications.
 *
 * This interface is intentionally small to keep the usage clear from the
 * SendMessage flow. Token resolution and mention target selection should
 * happen outside and call into this sender with already-resolved targets.
 */
public interface FcmSender {
    /** Sends an arbitrary data notification to multiple device tokens. */
    SendResult sendToTokens(List<String> tokens, Map<String, String> data, String title, String body);
}
