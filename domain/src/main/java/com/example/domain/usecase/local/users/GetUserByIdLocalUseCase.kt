package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.LocalUserRepository
import javax.inject.Inject

interface GetUserByIdLocalUseCase {
    suspend operator fun invoke(userId: DocumentId): CustomResult<User, Exception>
}

class GetUserByIdLocalUseCaseImpl @Inject constructor(
    private val localUserRepository: LocalUserRepository
) : GetUserByIdLocalUseCase {

    override suspend operator fun invoke(userId: DocumentId): CustomResult<User, Exception> {
        return TODO("로컬 저장소에서 ID로 사용자 정보 조회")
    }
} 