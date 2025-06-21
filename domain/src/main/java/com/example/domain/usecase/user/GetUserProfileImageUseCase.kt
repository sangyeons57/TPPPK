package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 사용자 프로필 이미지 URL을 가져오는 UseCase 인터페이스
 * 
 * 이 UseCase는 사용자 ID를 받아 해당 사용자의 프로필 이미지 URL을 Flow로 반환합니다.
 * 사용자 정보가 변경될 때마다 Flow를 통해 최신 프로필 이미지 URL을 받을 수 있습니다.
 */
interface GetUserProfileImageUseCase {
    /**
     * 사용자 ID를 받아 해당 사용자의 프로필 이미지 URL을 Flow로 반환합니다.
     * 
     * @param userId 프로필 이미지를 가져올 사용자의 ID
     * @return 프로필 이미지 URL을 포함한 Flow<CustomResult<String?, Exception>>
     */
    operator fun invoke(userId: String): Flow<CustomResult<String?, Exception>>
}

/**
 * 사용자 프로필 이미지 URL을 가져오는 UseCase 구현체
 * 
 * 이 UseCase는 사용자 ID를 받아 해당 사용자의 프로필 이미지 URL을 Flow로 반환합니다.
 * 사용자 정보가 변경될 때마다 Flow를 통해 최신 프로필 이미지 URL을 받을 수 있습니다.
 */
class GetUserProfileImageUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : GetUserProfileImageUseCase {
    /**
     * 사용자 ID를 받아 해당 사용자의 프로필 이미지 URL을 Flow로 반환합니다.
     * 
     * @param userId 프로필 이미지를 가져올 사용자의 ID
     * @return 프로필 이미지 URL을 포함한 Flow<CustomResult<String?, Exception>>
     */
    override operator fun invoke(userId: String): Flow<CustomResult<String?, Exception>> {
        // userRepository.getUserStream(userId)에서 반환하는 Flow를 변환하여 프로필 이미지 URL만 추출
        return userRepository.observe(userId).map { result ->
            when (result) {
                is CustomResult.Success -> {
                    CustomResult.Success(result.data.profileImageUrl?.value)
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(result.error)
                }
                else -> {
                    CustomResult.Failure(Exception("Unknown error"))
                }
            }
        }
    }
}
