package com.example.domain.usecase.dev

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain.repository.remote.AuthRepository
import javax.inject.Inject

/**
 * WebSocket 연결에 필요한 사용자 세션 정보를 가져오는 UseCase
 * 인증 상태 확인과 WebSocket 연결 준비 상태를 검증
 */
class GetWebSocketConnectionStatusUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    
    /**
     * WebSocket 연결을 위한 사용자 인증 상태를 확인합니다.
     * 
     * @return 사용자 세션 정보 또는 오류
     */
    suspend operator fun invoke(): CustomResult<UserSession, Exception> {
        return authRepository.getCurrentUserSession()
    }
    
    /**
     * WebSocket 연결을 위한 로그인 상태를 확인합니다.
     * 
     * @return 로그인 여부
     */
    suspend fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }
}