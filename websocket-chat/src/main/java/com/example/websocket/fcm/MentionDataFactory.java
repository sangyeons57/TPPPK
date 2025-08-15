package com.example.websocket.fcm;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds FCM data payload maps from primitive message fields.
 */
public final class MentionDataFactory {

    private final String deeplinkBase; // e.g., app://channel
    private final int snippetMaxLength;

    public MentionDataFactory(String deeplinkBase, int snippetMaxLength) {
        this.deeplinkBase = deeplinkBase;
        this.snippetMaxLength = snippetMaxLength <= 0 ? 100 : snippetMaxLength;
    }

    public Map<String, String> buildData(String channelType,
                                         String channelId,
                                         String messageId,
                                         String senderId,
                                         String senderName,
                                         String fullText,
                                         String targetUserId) {
        String snippet = toSnippet(fullText);
        String deeplink = String.format("%s/%s/%s?messageId=%s", deeplinkBase, channelType, channelId, messageId);

        Map<String, String> data = new HashMap<>();
        data.put("type", "mention");
        data.put("channelType", channelType);
        data.put("channelId", channelId);
        data.put("messageId", messageId);
        data.put("senderId", senderId);
        data.put("senderName", senderName);
        data.put("snippet", snippet);
        data.put("deeplink", deeplink);
        data.put("targetUserId", targetUserId);
        return data;
    }

    public String buildTitle(String senderName) {
        return senderName + " mentioned you"; // i18n can be applied client-side if using data-only
    }

    public String buildBody(String snippet) {
        return snippet;
    }

    private String toSnippet(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.length() <= snippetMaxLength) return trimmed;
        return trimmed.substring(0, Math.max(0, snippetMaxLength - 1)) + "\u2026"; // ellipsis
    }
}

