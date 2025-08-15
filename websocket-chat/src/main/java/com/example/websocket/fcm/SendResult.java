package com.example.websocket.fcm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result summary for an FCM send operation.
 */
public final class SendResult {
    public final int successCount;
    public final int failureCount;
    public final List<String> messageIds;   // per-token message ID if available
    public final List<String> invalidTokens; // tokens to delete when invalid

    private SendResult(int successCount, int failureCount, List<String> messageIds, List<String> invalidTokens) {
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.messageIds = messageIds == null ? Collections.emptyList() : Collections.unmodifiableList(messageIds);
        this.invalidTokens = invalidTokens == null ? Collections.emptyList() : Collections.unmodifiableList(invalidTokens);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int successCount = 0;
        private int failureCount = 0;
        private final List<String> messageIds = new ArrayList<>();
        private final List<String> invalidTokens = new ArrayList<>();

        public Builder addSuccess(String messageId) {
            this.successCount += 1;
            if (messageId != null) this.messageIds.add(messageId);
            return this;
        }

        public Builder addFailure(String maybeInvalidToken) {
            this.failureCount += 1;
            if (maybeInvalidToken != null) this.invalidTokens.add(maybeInvalidToken);
            return this;
        }

        public SendResult build() {
            return new SendResult(successCount, failureCount, messageIds, invalidTokens);
        }
    }
}

