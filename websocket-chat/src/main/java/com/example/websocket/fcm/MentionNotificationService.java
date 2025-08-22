package com.example.websocket.fcm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrates mention notification delivery from primitive fields, so you can
 * call this directly from your SendMessage flow without new message DTOs.
 */
public final class MentionNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(MentionNotificationService.class);

    private final FcmSender sender;
    private final FcmTokenRepository tokenRepository;
    private final MentionDataFactory dataFactory;

    public MentionNotificationService(FcmSender sender, FcmTokenRepository tokenRepository, MentionDataFactory dataFactory) {
        this.sender = sender;
        this.tokenRepository = tokenRepository;
        this.dataFactory = dataFactory;
    }

    /**
     * Sends mention notifications to the provided user IDs.
     * - Dedupe user IDs
     * - Exclude the sender
     * - Remove invalid tokens on failure responses
     */
    public void notifyMentions(String channelType,
                               String channelId,
                               String messageId,
                               String senderId,
                               String senderName,
                               String fullText,
                               List<String> mentionedUserIds) {
        if (mentionedUserIds == null || mentionedUserIds.isEmpty()) return;

        Set<String> uniqueTargets = new HashSet<>(mentionedUserIds);
        uniqueTargets.remove(senderId); // do not notify self

        logger.info("🔔 Mention notify: channelType={}, channelId={}, messageId={}, targets={} (sender={})",
                channelType, channelId, messageId, uniqueTargets.size(), senderId);

        for (String targetUserId : uniqueTargets) {
            try {
                List<String> tokens = tokenRepository.getTokensForUser(targetUserId);
                if (tokens == null || tokens.isEmpty()) {
                    logger.debug("🔕 No tokens for user {} — skipping mention", targetUserId);
                    continue;
                }

                String snippet = fullText == null ? "" : fullText.trim();
                Map<String, String> data = dataFactory.buildData(
                        channelType, channelId, messageId, senderId, senderName, snippet, targetUserId
                );

                String title = dataFactory.buildTitle(senderName);
                String body = dataFactory.buildBody(data.get("snippet"));

                SendResult result = sender.sendToTokens(tokens, data, title, body);
                logger.info("📊 Mention FCM result: userId={}, success={}, failure={}, invalidTokens={}",
                        targetUserId, result.successCount, result.failureCount, result.invalidTokens.size());

                if (!result.invalidTokens.isEmpty()) {
                    tokenRepository.removeInvalidTokens(targetUserId, result.invalidTokens);
                    logger.info("🧹 Removed {} invalid tokens for user {}", result.invalidTokens.size(), targetUserId);
                }
            } catch (Exception e) {
                logger.warn("⚠️ Mention notify failed for user {}: {}", targetUserId, e.getMessage());
            }
        }
    }
}
