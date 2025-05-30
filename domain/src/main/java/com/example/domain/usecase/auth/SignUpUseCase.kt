package com.example.domain.usecase.auth

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
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
    private val userRepository: UserRepository,
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
     * @param consentTimeStamp 서비스 정책및 개인정보처리방침 동의 시간 (기본값은 현재 시간)
     * @return 성공 시 사용자 정보가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(
        email: String, 
        password: String, 
        nickname: String,
        consentTimeStamp: Instant = Instant.now()
    ): CustomResult<User, Exception> {

        val uid = authRepository.signup(email, password)
        return when (uid){
            is CustomResult.Failure<*> ->
                TODO()
            is CustomResult.Success<String> -> {
                val user = User (
                    uid = uid.data,
                    email = email,
                    name = nickname,
                    consentTimeStamp = consentTimeStamp
                )
                return when ( userRepository.createUserProfile(user) ) {
                    is CustomResult.Failure<*> ->
                        return CustomResult.Failure<Exception>( Exception("Failed to create user profile"))
                    is CustomResult.Success<Unit> ->
                        return CustomResult.Success(user)
                    else -> {
                        return CustomResult.Failure<Exception>( Exception("Failed to create user profile"))
                    }

                }
            }
            else -> {
                TODO()
            }
        }
        // 회원가입 수행
    }
} 