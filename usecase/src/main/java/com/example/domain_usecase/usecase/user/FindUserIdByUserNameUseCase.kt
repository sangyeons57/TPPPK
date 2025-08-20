package com.example.domain_usecase.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.vo.user.UserName
import com.example.domain_repository.base.UserRepository
import javax.inject.Inject

/**
 * 사용자 이름으로 사용자 ID를 찾는 UseCase
 *
 * 기능:
 * - 주어진 사용자 이름으로 해당하는 사용자의 ID를 조회
 * - 사용자가 존재하지 않을 경우 적절한 오류 반환
 */
interface FindUserIdByUserNameUseCase {
    /**
     * 사용자 이름으로 사용자 ID 조회
     *
     * @param userName 찾을 사용자의 이름
     * @return 사용자 ID 또는 오류를 담은 CustomResult
     */
    suspend operator fun invoke(userName: UserName): CustomResult<String, Exception>
}

/**
 * 사용자 이름으로 사용자 ID 찾기 UseCase 구현체
 */
class FindUserIdByUserNameUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : FindUserIdByUserNameUseCase {

    override suspend operator fun invoke(userName: UserName): CustomResult<String, Exception> {
        return try {
            // 사용자 이름 유효성 검증
            if (userName.value.isBlank()) {
                return CustomResult.Failure(IllegalArgumentException("사용자 이름이 비어있습니다."))
            }

            // UserRepository를 통해 사용자 ID 조회
            userRepository.findUserIdByUserName(userName)

        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}