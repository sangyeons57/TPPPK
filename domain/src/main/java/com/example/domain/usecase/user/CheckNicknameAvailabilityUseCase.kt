package com.example.domain.usecase.user

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 닉네임 중복 확인을 수행하는 UseCase
 * 
 * @property userRepository 사용자 관련 기능을 제공하는 Repository
 */
class CheckNicknameAvailabilityUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * 지정된 닉네임의 사용 가능 여부를 확인합니다.
     *
     * @param nickname 확인할 닉네임
     * @return 성공 시 사용 가능 여부(Boolean)가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(nickname: String): CustomResult<Boolean, Exception> {
        Log.d("CheckNicknameAvailabilityUseCase", "invoke called with nickname: $nickname")
        return userRepository.checkNicknameAvailability(nickname)
    }
} 