package com.example.domain.model

/**
 * 사용자 세션 정보를 담는 데이터 클래스.
 *
 * @property userId 사용자의 고유 ID.
 * @property token 인증 토큰.
 * @property email 사용자 이메일 (선택적).
 * @property displayName 사용자 표시 이름 (선택적).
 * @property photoUrl 사용자 프로필 사진 URL (선택적).
 */
data class UserSession(
    val userId: String,
    val token: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null
    // Add other relevant session/user data as needed
)
