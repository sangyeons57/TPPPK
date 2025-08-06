package com.example.domain.model.data

import com.example.domain.vo.Token
import com.example.domain.vo.UserId
import com.example.domain.vo.user.UserEmail
import com.example.domain.vo.user.UserName
import java.time.Instant

/**
 * 사용자 세션 정보를 담는 데이터 클래스.
 *
 * @property userId 사용자의 고유 ID.
 * @property email 사용자 이메일 (선택적).
 * @property displayName 사용자 표시 이름 (선택적).
 * @property idToken Firebase ID 토큰. 앱의 백엔드 서버와 통신 시 사용됩니다.
 * @property tokenExpiresAt ID 토큰의 만료 시간 (선택적).
 */
data class UserSession(
    val userId: UserId,
    val email: UserEmail? = null,
    val displayName: UserName? = null,
    val idToken: Token? = null,
    val tokenExpiresAt: Instant? = null
) {

    /**
     * 현재 시점 기준으로 토큰이 만료되었는지 확인
     */
    fun isTokenExpired(): Boolean {
        return when {
            idToken == null -> true
            tokenExpiresAt == null -> false // 만료 시간 정보가 없으면 유효한 것으로 간주
            else -> Instant.now().isAfter(tokenExpiresAt)
        }
    }

    /**
     * 토큰이 곧 만료될 예정인지 확인 (기본 5분 전)
     */
    fun isTokenExpiringSoon(thresholdMinutes: Long = 5): Boolean {
        return when {
            idToken == null -> true
            tokenExpiresAt == null -> false
            else -> {
                val thresholdTime = Instant.now().plusSeconds(thresholdMinutes * 60)
                thresholdTime.isAfter(tokenExpiresAt)
            }
        }
    }

    /**
     * 토큰이 존재하고 유효한지 확인
     */
    fun hasValidToken(): Boolean {
        return idToken != null && !isTokenExpired()
    }

    /**
     * 토큰의 남은 유효 시간을 초 단위로 반환
     * @return 남은 시간(초), 만료된 경우 0, 정보가 없는 경우 null
     */
    fun getTokenRemainingTimeSeconds(): Long? {
        return when {
            idToken == null -> 0
            tokenExpiresAt == null -> null
            else -> {
                val remaining = tokenExpiresAt.epochSecond - Instant.now().epochSecond
                maxOf(0, remaining)
            }
        }
    }
}
