package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.repository.FunctionsRepository
import javax.inject.Inject

/**
 * 사용자 프로필을 업데이트하는 UseCase
 * Firebase Functions를 통해 이름, 메모 등의 프로필 정보를 업데이트합니다.
 */
class UpdateUserProfileUseCase @Inject constructor(
    private val functionsRepository: FunctionsRepository
) {
    /**
     * 사용자 프로필을 업데이트합니다.
     * 
     * @param name 새로운 사용자 이름 (nullable)
     * @param memo 새로운 사용자 메모 (nullable)
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend operator fun invoke(
        name: String? = null,
        memo: String? = null
    ): CustomResult<Unit, Exception> {
        return functionsRepository.updateUserProfile(name, memo)
    }
}