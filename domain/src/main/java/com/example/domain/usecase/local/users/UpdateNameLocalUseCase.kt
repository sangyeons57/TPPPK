package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.local.LocalUserRepository
import javax.inject.Inject

interface UpdateNameLocalUseCase {
    suspend operator fun invoke(newName: UserName): CustomResult<Unit, Exception>
}

class UpdateNameLocalUseCaseImpl @Inject constructor(
    private val localUserRepository: LocalUserRepository
) : UpdateNameLocalUseCase {

    override suspend operator fun invoke(newName: UserName): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 사용자 이름 업데이트")
    }
} 