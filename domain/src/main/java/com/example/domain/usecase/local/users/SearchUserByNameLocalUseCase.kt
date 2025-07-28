package com.example.domain.usecase.local.users

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.local.UserLocalRepository
import javax.inject.Inject

interface SearchUserByNameLocalUseCase {
    suspend operator fun invoke(name: UserName): CustomResult<User, Exception>
}

class SearchUserByNameLocalUseCaseImpl @Inject constructor(
    private val userLocalRepository: UserLocalRepository
) : SearchUserByNameLocalUseCase {

    override suspend operator fun invoke(name: UserName): CustomResult<User, Exception> {
        return TODO("로컬 저장소에서 이름으로 사용자 검색")
    }
} 