package com.example.data_model.remote

import com.example.domain.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.User
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import java.util.Date

/**
 * 사용자 정보를 나타내는 DTO 클래스
 */
data class UserDTO(
    @DocumentId override val id: String = "",
    @get:PropertyName(EMAIL)
    val email: String = "",
    @get:PropertyName(NAME)
    val name: String = "",
    @get:PropertyName(CONSENT_TIMESTAMP)
    @ServerTimestamp val consentTimeStamp: Date? = null,
    @get:PropertyName(MEMO)
    val memo: String? = null,
    @get:PropertyName(USER_STATUS)
    val status: UserStatus = UserStatus.OFFLINE, // "online", "offline", "away" 등
    @get:PropertyName(FCM_TOKEN)
    val fcmToken: String? = null,
    @get:PropertyName(ACCOUNT_STATUS)
    val accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE, // "active", "suspended", "deleted" 등
    @get:PropertyName(AggregateRoot.KEY_CREATED_AT)
    @get:ServerTimestamp override val createdAt: Date? = null,
    @get:PropertyName(AggregateRoot.KEY_UPDATED_AT)
    @get:ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {

        const val COLLECTION_NAME = User.COLLECTION_NAME
        const val EMAIL = User.KEY_EMAIL
        const val NAME = User.KEY_NAME
        const val CONSENT_TIMESTAMP = User.KEY_CONSENT_TIMESTAMP
        const val MEMO = User.KEY_MEMO
        const val USER_STATUS = User.KEY_USER_STATUS // User's online/offline status
        const val FCM_TOKEN = User.KEY_FCM_TOKEN
        const val ACCOUNT_STATUS = User.KEY_ACCOUNT_STATUS

    }
}

