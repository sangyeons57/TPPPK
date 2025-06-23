package com.example.domain.provider.auth

import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.usecase.auth.session.CheckAuthenticationStatusUseCase
import com.example.domain.usecase.auth.session.CheckSessionUseCase
import com.example.domain.usecase.auth.session.LoginUseCase
import com.example.domain.usecase.auth.session.LogoutUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 및 세션 관리 UseCase들을 제공하는 Provider
 * 
 * 로그인, 로그아웃, 세션 확인 등의 기본 인증 기능을 담당합니다.
 */
@Singleton
class AuthSessionUseCaseProvider @Inject constructor(
    private val authRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * 인증 및 세션 관리 UseCase들을 생성합니다.
     * 
     * @return 인증 세션 관리 UseCase 그룹
     */
    fun create(): AuthSessionUseCases {
        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        return AuthSessionUseCases(
            // 로그인/로그아웃
            loginUseCase = LoginUseCase(
                authRepository = authRepository
            ),
            
            logoutUseCase = LogoutUseCase(
                authRepository = authRepository
            ),
            
            // 세션 확인
            checkAuthenticationStatusUseCase = CheckAuthenticationStatusUseCase(
                authRepository = authRepository
            ),
            
            checkSessionUseCase = CheckSessionUseCase(
                authRepository = authRepository
            ),
            
            // 공통 Repository
            authRepository = authRepository
        )
    }
}

/**
 * 인증 세션 관리 UseCase 그룹
 */
data class AuthSessionUseCases(
    // 로그인/로그아웃
    val loginUseCase: LoginUseCase,
    val logoutUseCase: LogoutUseCase,
    
    // 세션 확인
    val checkAuthenticationStatusUseCase: CheckAuthenticationStatusUseCase,
    val checkSessionUseCase: CheckSessionUseCase,
    
    // 공통 Repository
    val authRepository: AuthRepository
)