package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import com.google.api.CustomHttpPatternOrBuilder
import kotlinx.coroutines.handleCoroutineException
import javax.inject.Inject
import kotlin.Result

/**
 * 사용자 닉네임을 업데이트하는 UseCase
 * 
 * @property userRepository 사용자 정보 관련 기능을 제공하는 Repository
 */
class UpdateNicknameUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 사용자의 닉네임을 업데이트합니다.
     * 닉네임은 공백을 제거하고, 길이 및 특수문자 등 유효성을 검사합니다.
     *
     * @param newNickname 변경할 새 닉네임
     * @return 성공 시 성공 결과가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(newNickname: String): CustomResult<Unit, Exception> {
        // 닉네임 유효성 검사
        val trimmedNickname = newNickname.trim()
        
        // 1. 빈 닉네임 검사
        if (trimmedNickname.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("닉네임은 비어있을 수 없습니다."))
        }
        
        // 2. 길이 검사 (2~20자 제한)
        if (trimmedNickname.length < 2 || trimmedNickname.length > 20) {
            return CustomResult.Failure(IllegalArgumentException("닉네임은 2~20자 사이여야 합니다."))
        }
        
        // 3. 특수문자 검사 (허용할 특수문자 정의)
        val allowedPattern = Regex("^[a-zA-Z0-9가-힣_.-]+$")
        if (!trimmedNickname.matches(allowedPattern)) {
            return CustomResult.Failure(IllegalArgumentException("닉네임에 허용되지 않는 특수문자가 포함되어 있습니다."))
        }
        
        // 4. 중복 검사 (UserRepository의 checkNicknameAvailability 활용)
        val availabilityResult = userRepository.checkNicknameAvailability(trimmedNickname)
        if (availabilityResult is CustomResult.Failure) {
            return CustomResult.Failure(availabilityResult.error)
        }
        
        if (availabilityResult !is CustomResult.Success) {
            return CustomResult.Failure(IllegalArgumentException("이미 사용 중인 닉네임입니다."))
        }

        val session = authRepository.getCurrentUserSession()
        // 유효성 검사 통과 후 Repository 호출
        if (session is CustomResult.Success) {
            return userRepository.updateUserProfile(session.data.userId, User(
                name = trimmedNickname,
            ))
        } else {
            return CustomResult.Failure(Exception("User not logged in"))
        }
    }
} 