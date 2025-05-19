package com.example.domain.usecase.user

import com.example.domain.model.User
import com.example.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 이름(닉네임)으로 사용자를 검색하는 UseCase입니다.
 */
class SearchUserByNameUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * 지정된 이름으로 사용자를 검색합니다.
     *
     * @param name 검색할 사용자 이름
     * @return 검색 결과에 해당하는 사용자 목록 또는 에러를 포함하는 Result
     */
    suspend operator fun invoke(name: String): Result<List<User>> {
        return userRepository.searchUsersByName(name)
    }
} 