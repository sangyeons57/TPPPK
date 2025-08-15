package com.example.websocket.fcm;

import java.util.List;

/**
 * Resolves and maintains device tokens for a user.
 */
public interface FcmTokenRepository {
    List<String> getTokensForUser(String userId) throws Exception;
    void removeInvalidTokens(String userId, List<String> invalidTokens) throws Exception;
}

