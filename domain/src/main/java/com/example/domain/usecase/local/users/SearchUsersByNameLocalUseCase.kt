package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.local.LocalUserRepository
import javax.inject.Inject

interface SearchUsersByNameLocalUseCase {
    suspend operator fun invoke(name: UserName): CustomResult<List<User>, Exception>
}

class SearchUsersByNameLocalUseCaseImpl @Inject constructor(
    private val localUserRepository: LocalUserRepository
) : SearchUsersByNameLocalUseCase {

    override suspend operator fun invoke(name: UserName): CustomResult<List<User>, Exception> {
        return TODO("로컬 저장소에서 이름으로 여러 사용자 검색")
    }
} 