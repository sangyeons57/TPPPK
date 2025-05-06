// 경로: domain/model/User.kt
package com.example.domain.model

import java.util.Date

/**
 * 사용자 정보를 담는 도메인 모델.
 * Firestore 'users' 컬렉션에 저장된 사용자 데이터를 표현합니다.
 */
data class User(
    val userId: String = "", // Firebase Auth UID
    val email: String = "",
    val name: String = "", // Unique
    val profileImageUrl: String? = null,
    val memo: String? = null, // User's personal memo/bio
    val status: UserStatus = UserStatus.OFFLINE,
    val createdAt: Date = Date(),
    val fcmToken: String? = null,
    val participatingProjectIds: List<String> = emptyList(),
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    val activeDmIds: List<String> = emptyList(),
    val isEmailVerified: Boolean = false
)

/**
 * 사용자 계정 상태를 나타내는 열거형.
 */
enum class AccountStatus {
    ACTIVE,     // 활성 계정
    SUSPENDED,  // 일시 정지된 계정
    DELETED     // 삭제된 계정
}
