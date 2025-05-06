package com.example.domain.usecase.auth

import com.example.domain.model.User
import com.example.domain.repository.AuthRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 사용자 로그인 기능을 수행하는 UseCase
 * 
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * 이메일과 비밀번호를 이용하여 사용자 로그인을 수행합니다.
     *
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return 성공 시 사용자 정보가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(email: String, password: String): Result<User?> {
        return authRepository.login(email, password)
    }
} 