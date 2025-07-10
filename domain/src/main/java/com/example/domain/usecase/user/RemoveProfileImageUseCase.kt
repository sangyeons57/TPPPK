package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import javax.inject.Inject

/**
 * 사용자 프로필 이미지를 제거하는 UseCase 인터페이스
 */
interface RemoveProfileImageUseCase {
    /**
     * 사용자의 프로필 이미지를 제거합니다.
     *
     * @return 성공 시 Unit이 포함된 CustomResult.Success, 실패 시 Exception이 포함된 CustomResult.Failure
     */
    suspend operator fun invoke(): CustomResult<String, Exception>
}

/**
 * 사용자 프로필 이미지를 제거하는 UseCase 구현체
 *
 * @property userRepository 사용자 정보 관련 기능을 제공하는 Repository
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class RemoveProfileImageUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : RemoveProfileImageUseCase {
    /**
     * 사용자의 프로필 이미지를 제거합니다.
     * Firebase Functions를 통해 서버에서 프로필 이미지를 삭제합니다.
     *
     * @return 성공 시 성공 메시지가 포함된 CustomResult.Success, 실패 시 Exception이 포함된 CustomResult.Failure
     */
    override suspend operator fun invoke(): CustomResult<String, Exception> {
        return try {
            // Firebase Functions를 통해 프로필 이미지 삭제
            val result = userRepository.removeProfileImage()
            
            when (result) {
                is CustomResult.Success -> {
                    CustomResult.Success("Profile image removed successfully")
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(result.error)
                }
                else -> {
                    CustomResult.Failure(Exception("Unknown error occurred during profile image removal"))
                }
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}