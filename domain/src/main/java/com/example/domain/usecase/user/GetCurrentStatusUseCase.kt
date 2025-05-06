package com.example.domain.usecase.user

import com.example.domain.model.UserStatus
import com.example.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 현재 사용자의 상태를 가져오는 UseCase
 * 
 * @property userRepository 사용자 관련 기능을 제공하는 Repository
 */
class GetCurrentStatusUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * 현재 사용자의 상태를 가져옵니다.
     *
     * @return 성공 시 사용자 상태가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(): Result<UserStatus> {
        return userRepository.getCurrentStatus()
    }
} 