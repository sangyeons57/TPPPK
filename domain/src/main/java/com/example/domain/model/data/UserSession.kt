package com.example.domain.model.data

import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.Token
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserName
import java.time.Instant

/**
 * 사용자 세션 정보를 담는 데이터 클래스.
 *
 * @property userId 사용자의 고유 ID.
 * @property email 사용자 이메일 (선택적).
 * @property displayName 사용자 표시 이름 (선택적).
 * @property idToken Firebase ID 토큰. 앱의 백엔드 서버와 통신 시 사용됩니다.
 */
data class UserSession(
    val userId: UserId,
    val email: UserEmail? = null,
    val displayName: UserName? = null,
    val idToken: Token? = null
)
