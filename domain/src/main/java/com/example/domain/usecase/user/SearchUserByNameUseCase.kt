package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 이름(닉네임)으로 사용자를 검색하는 UseCase입니다.
 */
interface SearchUserByNameUseCase {
    suspend operator fun invoke(name: UserName): Flow<CustomResult<User, Exception>>
}

class SearchUserByNameUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
): SearchUserByNameUseCase {

    /**
     * 지정된 이름으로 사용자를 검색합니다.
     *
     * @param name 검색할 사용자 이름
     * @return 검색 결과에 해당하는 사용자 목록 또는 에러를 포함하는 Result
     */
    override suspend operator fun invoke(name: UserName): Flow<CustomResult<User, Exception>> {
        return userRepository.observeByName(name)
    }
} 