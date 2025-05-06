package com.example.domain.usecase.auth

import com.example.domain.model.User
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 사용자 회원가입 기능을 수행하는 UseCase
 * 
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 * @property userRepository 사용자 관련 기능을 제공하는 Repository
 */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    /**
     * 이메일, 비밀번호, 닉네임을 이용하여 회원가입을 수행합니다.
     * 1. Firebase Authentication에 계정 생성
     * 2. 닉네임 중복 확인 (선택적)
     * 3. Firestore에 사용자 프로필 생성
     *
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @param nickname 사용자 닉네임
     * @return 성공 시 사용자 정보가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(
        email: String, 
        password: String, 
        nickname: String
    ): Result<User?> {
        // 닉네임 중복 확인 (선택적)
        val nicknameCheck = userRepository.checkNicknameAvailability(nickname)
        if (nicknameCheck.isFailure || nicknameCheck.getOrNull() == false) {
            return Result.failure(IllegalArgumentException("이미 사용 중인 닉네임입니다."))
        }
        
        // 회원가입 수행
        return authRepository.signUp(email, password, nickname)
    }
} 