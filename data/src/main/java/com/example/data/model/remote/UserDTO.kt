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
import com.example.domain.model.vo.DocumentId as VODocumentId
import java.time.Instant

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
    val consentTimeStamp: Timestamp? = null,
    @get:PropertyName(PROFILE_IMAGE_URL)
    val profileImageUrl: String? = null,
    @get:PropertyName(MEMO)
    val memo: String? = null,
    @get:PropertyName(USER_STATUS)
    val status: UserStatus = UserStatus.OFFLINE, // "online", "offline", "away" 등
    @get:PropertyName(CREATED_AT)
    val createdAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(FCM_TOKEN)
    val fcmToken: String? = null,
    @get:PropertyName(ACCOUNT_STATUS)
    val accountStatus: UserAccountStatus = UserAccountStatus.ACTIVE // "active", "suspended", "deleted" 등
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
                consentTimeStamp = DateTimeUtil.instantToFirebaseTimestamp(domain.consentTimeStamp),
                profileImageUrl = domain.profileImageUrl?.value,
                memo = domain.memo?.value,
                status = domain.userStatus,
                createdAt = DateTimeUtil.instantToFirebaseTimestamp(domain.createdAt),
                updatedAt = DateTimeUtil.instantToFirebaseTimestamp(domain.updatedAt),
                fcmToken = domain.fcmToken?.value,
                accountStatus = domain.accountStatus
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
            consentTimeStamp = consentTimeStamp?.let{DateTimeUtil.firebaseTimestampToInstant(it)} ?: Instant.EPOCH, // Provide a default if null
            profileImageUrl = profileImageUrl.takeIf { !it.isNullOrBlank() }?.let { ImageUrl(it) }, // Wrap in Value Object.ImageUrl(profileImageUrl),
            memo = memo?.let { UserMemo(it) }, // Wrap in Value Object
            userStatus = status ,
            createdAt = DateTimeUtil.firebaseTimestampToInstant(createdAt), // Provide a default if null
            updatedAt = DateTimeUtil.firebaseTimestampToInstant(updatedAt), // Provide a default if null
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
        consentTimeStamp = DateTimeUtil.instantToFirebaseTimestamp(consentTimeStamp), // consentTimeStamp is non-null in User
        profileImageUrl = profileImageUrl?.value,
        memo = memo?.value,  // Extract primitive value if memo is not null
        status = userStatus, // Corrected from 'status' to 'userStatus'
        createdAt = DateTimeUtil.instantToFirebaseTimestamp(createdAt), // createdAt is non-null in User
        updatedAt = DateTimeUtil.instantToFirebaseTimestamp(updatedAt), // updatedAt is non-null in User
        fcmToken = fcmToken?.value,
        accountStatus = accountStatus
    )
}