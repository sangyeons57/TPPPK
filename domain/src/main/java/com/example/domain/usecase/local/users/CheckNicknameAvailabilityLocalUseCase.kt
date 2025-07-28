package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.local.UserLocalRepository
import javax.inject.Inject

interface CheckNicknameAvailabilityLocalUseCase {
    suspend operator fun invoke(nickname: UserName): CustomResult<Boolean, Exception>
}

class CheckNicknameAvailabilityLocalUseCaseImpl @Inject constructor(
    private val userLocalRepository: UserLocalRepository
) : CheckNicknameAvailabilityLocalUseCase {

    override suspend operator fun invoke(nickname: UserName): CustomResult<Boolean, Exception> {
        return TODO("로컬 저장소에서 닉네임 사용 가능 여부 확인")
    }
}