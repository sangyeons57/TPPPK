package com.example.domain.usecase.user

import com.example.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 사용자 프로필 이미지를 제거하는 UseCase
 * 
 * @property userRepository 사용자 정보 관련 기능을 제공하는 Repository
 */
class RemoveProfileImageUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * 사용자의 프로필 이미지를 제거합니다.
     *
     * @return 성공 시 성공 결과가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(): Result<Unit> {
        return userRepository.removeProfileImage()
    }
} 