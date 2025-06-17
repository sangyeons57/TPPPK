package com.example.data.model.remote

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.base.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.example.core_common.constants.FirestoreConstants
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserFcmToken
import com.example.domain.model.vo.user.UserMemo
import com.example.domain.model.vo.user.UserName
import java.time.Instant

/**
 * 사용자 정보를 나타내는 DTO 클래스
 */
data class UserDTO(
    @DocumentId val uid: String = "",
    @get:PropertyName(FirestoreConstants.Users.EMAIL)
    val email: String = "",
    @get:PropertyName(FirestoreConstants.Users.NAME)
    val name: String = "",
    @get:PropertyName(FirestoreConstants.Users.CONSENT_TIMESTAMP)
    val consentTimeStamp: Timestamp? = null,
    @get:PropertyName(FirestoreConstants.Users.PROFILE_IMAGE_URL)
    val profileImageUrl: String? = null,
    @get:PropertyName(FirestoreConstants.Users.MEMO)
    val memo: String? = null,
    @get:PropertyName(FirestoreConstants.Users.STATUS)
    val status: String = "offline", // "online", "offline", "away" 등
    @get:PropertyName(FirestoreConstants.Users.CREATED_AT)
    val createdAt: Timestamp? = null,
    @get:PropertyName(FirestoreConstants.Users.UPDATED_AT)
    val updatedAt: Timestamp? = null,
    @get:PropertyName(FirestoreConstants.Users.FCM_TOKEN)
    val fcmToken: String? = null,
    @get:PropertyName(FirestoreConstants.Users.ACCOUNT_STATUS)
    val accountStatus: String = "active" // "active", "suspended", "deleted" 등
) {
    /**
     * DTO를 도메인 모델로 변환
     * @return User 도메인 모델
     */
    fun toDomain(): User {
        return User.fromDataSource(
            uid = com.example.domain.model.vo.DocumentId(uid),
            email = UserEmail(email), // Wrap in Value Object
            name = UserName(name),   // Wrap in Value Object
            consentTimeStamp = consentTimeStamp?.let{DateTimeUtil.firebaseTimestampToInstant(it)} ?: Instant.EPOCH, // Provide a default if null
            profileImageUrl = profileImageUrl.takeIf { !it.isNullOrBlank() }?.let { ImageUrl(it) }, // Wrap in Value Object.ImageUrl(profileImageUrl),
            memo = memo?.let { UserMemo(it) }, // Wrap in Value Object
            userStatus = try {
                UserStatus.valueOf(status.uppercase())
            } catch (e: Exception) {
                UserStatus.OFFLINE // Default to OFFLINE if parsing fails
            },
            createdAt = createdAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)} ?: Instant.EPOCH, // Provide a default if null
            updatedAt = updatedAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)} ?: Instant.EPOCH, // Provide a default if null
            fcmToken = UserFcmToken(fcmToken),
            accountStatus = try {
                UserAccountStatus.valueOf(accountStatus.uppercase())
            } catch (e: Exception) {
                UserAccountStatus.ACTIVE // Default to ACTIVE if parsing fails
            }
        )
    }
}

/**
 * User 도메인 모델을 DTO로 변환하는 확장 함수
 * @return UserDTO 객체
 */
fun User.toDto(): UserDTO {
    return UserDTO(
        uid = uid.value,
        email = email.value, // Extract primitive value
        name = name.value,   // Extract primitive value
        consentTimeStamp = DateTimeUtil.instantToFirebaseTimestamp(consentTimeStamp), // consentTimeStamp is non-null in User
        profileImageUrl = profileImageUrl?.value,
        memo = memo?.value,  // Extract primitive value if memo is not null
        status = userStatus.name.lowercase(), // Corrected from 'status' to 'userStatus'
        createdAt = DateTimeUtil.instantToFirebaseTimestamp(createdAt), // createdAt is non-null in User
        updatedAt = DateTimeUtil.instantToFirebaseTimestamp(updatedAt), // updatedAt is non-null in User
        fcmToken = fcmToken?.value,
        accountStatus = accountStatus.name.lowercase()
    )
}