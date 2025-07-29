package com.example.domain.provider

import com.example.domain.repository.local.LocalUserRepository
import com.example.domain.repository.remote.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 로그인, 세션 관리, 계정 관리 등의 기능을 담당합니다.
 */
@Singleton
class AuthUseCaseProvider @Inject constructor(
    private val authRepository: AuthRepository,
    private val localUserRepository: LocalUserRepository
) {

    /**
     * 세션 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 세션 관리 UseCase 그룹
     */
    fun createSessionUseCases(): AuthLocalSessionUseCases {
        return AuthLocalSessionUseCases(
            // TODO: 향후 local auth session use cases 추가
            authRepository = authRepository,
            localUserRepository = localUserRepository
        )
    }

    /**
     * 계정 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 계정 관리 UseCase 그룹
     */
    fun createAccountUseCases(): AuthLocalAccountUseCases {
        return AuthLocalAccountUseCases(
            // TODO: 향후 local auth account use cases 추가
            authRepository = authRepository,
            localUserRepository = localUserRepository
        )
    }

    /**
     * 유효성 검사 관련 UseCase들을 생성합니다.
     * 
     * @return 유효성 검사 UseCase 그룹
     */
    fun createValidationUseCases(): AuthLocalValidationUseCases {
        return AuthLocalValidationUseCases(
            // TODO: 향후 local auth validation use cases 추가
            authRepository = authRepository,
            localUserRepository = localUserRepository
        )
    }
}

/**
 * 세션 관리 Local UseCase 그룹
 */
data class AuthLocalSessionUseCases(
    val authRepository: AuthRepository,
    val localUserRepository: LocalUserRepository
)

/**
 * 계정 관리 Local UseCase 그룹
 */
data class AuthLocalAccountUseCases(
    val authRepository: AuthRepository,
    val localUserRepository: LocalUserRepository
)

/**
 * 유효성 검사 Local UseCase 그룹
 */
data class AuthLocalValidationUseCases(
    val authRepository: AuthRepository,
    val localUserRepository: LocalUserRepository
) 