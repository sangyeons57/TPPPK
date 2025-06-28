package com.example.data.model.remote

import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.base.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserFcmToken
import com.example.domain.model.vo.user.UserMemo
import com.example.domain.model.vo.user.UserName
import com.google.firebase.firestore.ServerTimestamp
import com.example.domain.model.vo.DocumentId as VODocumentId
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
    @get:PropertyName(PROFILE_IMAGE_URL)
    val profileImageUrl: String? = null,
    @get:PropertyName(MEMO)
    val memo: String? = null,
    @get:PropertyName(USER_STATUS)
    val status: UserStatus = UserStatus.OFFLINE, // "online", "offline", "away" 등
    @get:PropertyName(FCM_TOKEN)
    val fcmToken: String? = null,
    @get:PropertyName(ACCOUNT_STATUS)
    val accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE, // "active", "suspended", "deleted" 등
    @ServerTimestamp override val createdAt: Date? = null,
    @ServerTimestamp override val updatedAt: Date? = null
) : DTO {

    companion object {

        const val COLLECTION_NAME = User.COLLECTION_NAME
        const val EMAIL = User.KEY_EMAIL
        const val NAME = User.KEY_NAME
        const val CONSENT_TIMESTAMP = User.KEY_CONSENT_TIMESTAMP
        const val PROFILE_IMAGE_URL = User.KEY_PROFILE_IMAGE_URL
        const val MEMO = User.KEY_MEMO
        const val USER_STATUS = User.KEY_USER_STATUS // User's online/offline status
        const val CREATED_AT = User.KEY_CREATED_AT
        const val UPDATED_AT = User.KEY_UPDATED_AT
        const val FCM_TOKEN = User.KEY_FCM_TOKEN
        const val ACCOUNT_STATUS = User.KEY_ACCOUNT_STATUS

        fun from(domain: User) : UserDTO{
            return UserDTO(
                id = domain.id.value,
                email = domain.email.value,
                name = domain.name.value,
                consentTimeStamp = Date.from(domain.consentTimeStamp), // Convert Instant to Date
                profileImageUrl = domain.profileImageUrl?.value,
                memo = domain.memo?.value,
                status = domain.userStatus,
                fcmToken = domain.fcmToken?.value,
                accountStatus = domain.accountStatus,
                createdAt = Date.from(domain.createdAt), // Convert Instant to Date
                updatedAt = Date.from(domain.updatedAt) // Convert Instant to Date
            )
        }
    }
    /**
     * DTO를 도메인 모델로 변환
     * @return User 도메인 모델
     */
    override fun toDomain(): User {
        return User.fromDataSource(
            id = VODocumentId(id),
            email = UserEmail(email), // Wrap in Value Object
            name = UserName(name),   // Wrap in Value Object
            consentTimeStamp = consentTimeStamp?.toInstant() ?: Instant.EPOCH, // Convert Date to Instant
            profileImageUrl = profileImageUrl.takeIf { !it.isNullOrBlank() }?.let { ImageUrl(it) }, // Wrap in Value Object.ImageUrl(profileImageUrl),
            memo = memo?.let { UserMemo(it) }, // Wrap in Value Object
            userStatus = status ,
            createdAt = createdAt?.toInstant() ?: Instant.EPOCH, // Convert Date to Instant with fallback
            updatedAt = updatedAt?.toInstant() ?: Instant.EPOCH, // Convert Date to Instant with fallback
            fcmToken = UserFcmToken(fcmToken),
            accountStatus = accountStatus
        )
    }
}

/**
 * User 도메인 모델을 DTO로 변환하는 확장 함수
 * @return UserDTO 객체
 */
fun User.toDto(): UserDTO {
    return UserDTO(
        id = id.value,
        email = email.value, // Extract primitive value
        name = name.value,   // Extract primitive value
        consentTimeStamp = Date.from(consentTimeStamp), // Convert Instant to Date
        profileImageUrl = profileImageUrl?.value,
        memo = memo?.value,  // Extract primitive value if memo is not null
        status = userStatus, // Corrected from 'status' to 'userStatus'
        fcmToken = fcmToken?.value,
        accountStatus = accountStatus,
        createdAt = Date.from(createdAt), // Convert Instant to Date
        updatedAt = Date.from(updatedAt) // Convert Instant to Date
    )
}