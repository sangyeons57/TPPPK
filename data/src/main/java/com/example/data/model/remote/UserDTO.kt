package com.example.data.model.remote

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.base.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * 사용자 정보를 나타내는 DTO 클래스
 */
data class UserDTO(
    @DocumentId val uid: String = "",
    val email: String = "",
    val name: String = "",
    val consentTimeStamp: Timestamp? = null,
    val profileImageUrl: String? = null,
    val memo: String? = null,
    val status: String = "offline", // "online", "offline", "away" 등
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val fcmToken: String? = null,
    val accountStatus: String = "active" // "active", "suspended", "deleted" 등
) {
    /**
     * DTO를 도메인 모델로 변환
     * @return User 도메인 모델
     */
    fun toDomain(): User {
        return User(
            uid = uid,
            email = email,
            name = name,
            consentTimeStamp = consentTimeStamp?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            profileImageUrl = profileImageUrl,
            memo = memo,
            status = try {
                UserStatus.valueOf(status.uppercase())
            } catch (e: Exception) {
                UserStatus.OFFLINE
            },
            createdAt = createdAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            updatedAt = updatedAt?.let{DateTimeUtil.firebaseTimestampToInstant(it)},
            fcmToken = fcmToken,
            accountStatus = try {
                UserAccountStatus.valueOf(accountStatus.uppercase())
            } catch (e: Exception) {
                UserAccountStatus.ACTIVE
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
        uid = uid,
        email = email,
        name = name,
        consentTimeStamp = consentTimeStamp?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        profileImageUrl = profileImageUrl,
        memo = memo,
        status = status.name.lowercase(),
        createdAt = createdAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        updatedAt = updatedAt?.let{DateTimeUtil.instantToFirebaseTimestamp(it)},
        fcmToken = fcmToken,
        accountStatus = accountStatus.name.lowercase()
    )
}