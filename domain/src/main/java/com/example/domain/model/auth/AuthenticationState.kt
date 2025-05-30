package com.example.domain.model.auth

import com.example.domain.model.base.User

/**
 * 사용자의 현재 인증 상태를 나타냅니다.
 * 이 상태는 앱 전체에서 사용자의 로그인 여부, 이메일 인증 필요 여부 등을 파악하는 데 사용됩니다.
 */
sealed interface AuthenticationState {
    /**
     * 초기 상태 또는 인증 상태를 아직 확인할 수 없는 경우입니다.
     * 앱 시작 시 일시적으로 이 상태일 수 있습니다.
     */
    data object Unknown : AuthenticationState

    /**
     * 사용자가 인증되지 않은 (로그아웃된) 상태입니다.
     */
    data object Unauthenticated : AuthenticationState

    /**
     * 사용자가 로그인했지만, 이메일 인증 절차를 완료하지 않은 상태입니다.
     * @property user 현재 사용자 정보 (이메일 등 인증에 필요한 최소 정보 포함 가능)
     */
    data class EmailVerificationNeeded(val user: User?) : AuthenticationState

    /**
     * 사용자가 성공적으로 인증된 (로그인 및 모든 필요 절차 완료) 상태입니다.
     * @property user 현재 로그인된 사용자의 전체 정보.
     */
    data class Authenticated(val user: User) : AuthenticationState
}
