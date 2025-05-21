// 경로: domain/model/User.kt
package com.example.domain.model

import java.time.Instant
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.AccountStatus
import com.google.firebase.firestore.DocumentId

/**
 * 사용자 정보를 담는 도메인 모델.
 * Firestore 'users' 컬렉션에 저장된 사용자 데이터를 표현합니다.
 */
data class User(
    @DocumentId val id: String = "",
    val email: String = "",
    val name: String = "", // Unique
    val profileImageUrl: String? = null,
    val memo: String? = null, // User's personal memo/bio
    val statusMessage: String? = null, // User's status message
    val status: UserStatus = UserStatus.OFFLINE,
    val createdAt: Instant = DateTimeUtil.nowInstant(),
    val fcmToken: String? = null,
    val participatingProjectIds: List<String> = emptyList(),
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    val activeDmIds: List<String> = emptyList(),
    val isEmailVerified: Boolean = false,
    val updatedAt: Instant? = null, // 사용자 정보 마지막 업데이트 시간
    val consentTimeStamp: Instant? = null // 서비스 정책및 개인정보처리방침 동의 시간
) {
    companion object {
        /**
         * 비어 있거나 초기화되지 않은 사용자 상태를 나타내는 User 객체입니다.
         */
        val EMPTY = User(
            id = "",
            email = "",
            name = "",
            profileImageUrl = null,
            memo = null,
            statusMessage = null,
            status = UserStatus.OFFLINE,
            createdAt = DateTimeUtil.nowInstant(), // 생성 시간은 현재로 초기화하거나 특정 값 사용
            fcmToken = null,
            participatingProjectIds = emptyList(),
            accountStatus = AccountStatus.UNKNOWN, // 또는 적절한 기본 AccountStatus
            activeDmIds = emptyList(),
            isEmailVerified = false,
            updatedAt = null,
            consentTimeStamp = null
        )
    }
    
    /**
     * User 객체를 UI 표시용 UserProfileData로 변환하는 함수
     * @return UserProfileData UI에서 사용할 사용자 프로필 데이터
     */
     fun toUserProfileData(): UserProfileData {
        return UserProfileData(
            id = this.id,
            name = this.name,
            email = this.email, // User.email is non-nullable, UserProfileData.email is nullable. This is fine.
            profileImageUrl = this.profileImageUrl,
            statusMessage = this.statusMessage // User.statusMessage is nullable, UserProfileData.statusMessage is nullable.
        )
    }
}

/**
 * UI-specific representation of a user's profile, derived from the User domain model.
 * Its constructor is internal to ensure it's created via User.toUserProfileData().
 */
data class UserProfileData internal constructor(
    val id: String,
    val name: String,
    val email: String?, // Email might be nullable or not always available from all sources
    val profileImageUrl: String?,
    val statusMessage: String?
)
