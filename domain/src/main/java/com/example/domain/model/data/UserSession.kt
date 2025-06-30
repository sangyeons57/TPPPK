package com.example.domain.model.data

import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserName

/**
 * 사용자 세션 정보를 담는 데이터 클래스.
 *
 * @property userId 사용자의 고유 ID.
 * @property email 사용자 이메일 (선택적).
 * @property displayName 사용자 표시 이름 (선택적).
 * @property photoUrl 사용자 프로필 사진 URL (선택적).
 */
data class UserSession(
    val userId: UserId,
    // Token field removed. Use AuthRepository.fetchIdToken() when an ID token is required.
    val email: UserEmail? = null,
    val displayName: UserName? = null,
    val photoUrl: ImageUrl? = null
    // Add other relevant session/user data as needed
)