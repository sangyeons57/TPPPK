package com.example.domain.usecase.auth.session

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.AuthRepository
import javax.inject.Inject

/**
 * 사용자 로그아웃을 처리하는 UseCase
 * 
 * 권한 문제나 인증 만료로 인한 자동 로그아웃 등의 상황을 고려하여
 * 모든 캐시와 저장된 정보를 정리하는 완전 로그아웃을 수행합니다.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 완전 로그아웃을 실행합니다.
     * Firebase Auth 로그아웃과 함께 모든 캐시와 로컬 데이터를 정리합니다.
     * 
     * @return 성공 시 CustomResult.Success(Unit), 실패 시 CustomResult.Failure(Exception)
     */
    suspend operator fun invoke(): CustomResult<Unit, Exception> {
        Log.d("LogoutUseCase", "Starting complete logout process")
        
        return when (val result = authRepository.logoutCompletely()) {
            is CustomResult.Success -> {
                Log.d("LogoutUseCase", "Complete logout successful")
                result
            }
            is CustomResult.Failure -> {
                Log.e("LogoutUseCase", "Complete logout failed", result.error)
                result
            }
            else -> {
                Log.e("LogoutUseCase", "Unexpected logout result: $result")
                CustomResult.Failure(Exception("Unexpected logout result"))
            }
        }
    }
}
