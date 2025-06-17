package com.example.domain.model.vo.user

/**
 * Represents a Firebase Cloud Messaging token for push notifications.
 * Token may be null (user logged out or no device token), but when present enforce length.
 */
@JvmInline
value class UserFcmToken(val value: String?) {
    init {
        value?.let {
            require(it.length <= MAX_LENGTH) { "FCM token cannot exceed $MAX_LENGTH characters." }
        }
    }

    companion object {
        const val MAX_LENGTH = 4096 // per FCM guidelines
    }
}
