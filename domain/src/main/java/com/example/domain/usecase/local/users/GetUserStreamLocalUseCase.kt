package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.UserLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GetUserStreamLocalUseCase {
    operator fun invoke(userId: DocumentId): Flow<CustomResult<User, Exception>>
}

class GetUserStreamLocalUseCaseImpl @Inject constructor(
    private val userLocalRepository: UserLocalRepository
) : GetUserStreamLocalUseCase {

    override operator fun invoke(userId: DocumentId): Flow<CustomResult<User, Exception>> {
        return TODO("로컬 저장소에서 특정 사용자 정보를 실시간으로 관찰")
    }
} 