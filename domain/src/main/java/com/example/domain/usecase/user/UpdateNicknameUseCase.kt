package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.user.UserName
import com.example.domain.model.ui.auth.NicknameValidationResult
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import com.example.domain.usecase.auth.ValidateNicknameForSignUpUseCase


import javax.inject.Inject


/**
 * 사용자 닉네임을 업데이트하는 UseCase
 * 
 * @property userRepository 사용자 정보 관련 기능을 제공하는 Repository
 */
class UpdateNicknameUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val validateNicknameForSignUpUseCase: ValidateNicknameForSignUpUseCase
) {
    /**
     * 사용자의 닉네임을 업데이트합니다.
     * 닉네임은 공백을 제거하고, 길이 및 특수문자 등 유효성을 검사합니다.
     *
     * @param newNickname 변경할 새 닉네임
     * @return 성공 시 성공 결과가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(newNickname: String): CustomResult<com.example.domain.model.base.User, Exception> {
        val trimmedNickname = newNickname.trim()

        // 1. Use ValidateNicknameForSignUpUseCase to check the nickname
        when (val validationResult = validateNicknameForSignUpUseCase(trimmedNickname)) {
            is NicknameValidationResult.Valid -> {
                // Nickname is valid and available, proceed with update
            }
            is NicknameValidationResult.NicknameAlreadyExists -> {
                return CustomResult.Failure(IllegalArgumentException("이미 사용 중인 닉네임입니다."))
            }
            is NicknameValidationResult.Failure -> {
                return CustomResult.Failure(Exception(validationResult.message))
            }
            else -> {
                // Handle other validation failures like TooShort, TooLong, InvalidCharacters, etc.
                val errorMessage = when (validationResult) {
                    is NicknameValidationResult.Empty -> "닉네임은 비어있을 수 없습니다."
                    is NicknameValidationResult.TooShort -> "닉네임이 너무 짧습니다. (최소 ${validationResult.minimumLength}자)"
                    is NicknameValidationResult.TooLong -> "닉네임이 너무 깁니다. (최대 ${validationResult.maximumLength}자)"
                    is NicknameValidationResult.InvalidCharacters -> "닉네임에 허용되지 않는 문자가 포함되어 있습니다."
                    else -> "알 수 없는 닉네임 유효성 검사 오류입니다."
                }
                return CustomResult.Failure(IllegalArgumentException(errorMessage))
            }
        }

        // 2. Get current user session
        val sessionResult = authRepository.getCurrentUserSession()
        val session = (sessionResult as? CustomResult.Success)?.data
            ?: return CustomResult.Failure(Exception("User not logged in"))

        // 3. Fetch user and update name
        return when (val userResult = userRepository.findById(session.userId)) {
            is CustomResult.Success -> {
                val user = userResult.data
                user.changeName(UserName(trimmedNickname))
                userRepository.save(user)
                CustomResult.Success(user)
            }
            is CustomResult.Failure -> {
                CustomResult.Failure(userResult.error)
            }
            else -> {
                CustomResult.Failure(Exception("User not found or other error"))
            }
        }
    }
} 