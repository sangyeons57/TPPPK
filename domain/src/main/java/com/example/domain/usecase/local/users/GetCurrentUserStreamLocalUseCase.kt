package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.local.UserLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetCurrentUserStreamLocalUseCase {
    operator fun invoke(): Flow<CustomResult<User, Exception>>
}

class GetCurrentUserStreamLocalUseCaseImpl @Inject constructor(
    private val userLocalRepository: UserLocalRepository
) : GetCurrentUserStreamLocalUseCase {

    override operator fun invoke(): Flow<CustomResult<User, Exception>> {
        return TODO("로컬 저장소에서 현재 사용자 정보를 실시간으로 관찰")
    }
} 