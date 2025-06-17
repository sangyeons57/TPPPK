package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 사용자 프로필 이미지를 제거하는 UseCase 인터페이스
 */
interface RemoveProfileImageUseCase {
    /**
     * 사용자의 프로필 이미지를 제거합니다.
     *
     * @return 성공 시 Unit이 포함된 CustomResult.Success, 실패 시 Exception이 포함된 CustomResult.Failure
     */
    suspend operator fun invoke(): CustomResult<String, Exception>
}

/**
 * 사용자 프로필 이미지를 제거하는 UseCase 구현체
 *
 * @property userRepository 사용자 정보 관련 기능을 제공하는 Repository
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class RemoveProfileImageUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : RemoveProfileImageUseCase {
    /**
     * 사용자의 프로필 이미지를 제거합니다.
     * 1. 현재 사용자 세션을 확인합니다.
     * 2. 세션이 유효하면 사용자 ID로 User 객체를 조회합니다.
     * 3. User 객체의 프로필 이미지를 제거하는 도메인 로직(`changeProfileImage(null)`)을 호출합니다.
     * 4. 변경된 User 객체를 저장소(`userRepository.save()`)를 통해 저장합니다.
     * 5. 저장 결과를 반환합니다.
     *
     * @return 성공 시 Unit이 포함된 CustomResult.Success, 실패 시 Exception이 포함된 CustomResult.Failure
     */
    override suspend operator fun invoke(): CustomResult<String, Exception> {
        val sessionResult = authRepository.getCurrentUserSession()
        val userId = when (sessionResult) {
            is CustomResult.Success -> sessionResult.data.userId
            is CustomResult.Failure -> return CustomResult.Failure(sessionResult.error ?: Exception("User not logged in"))
            // Assuming getCurrentUserSession, as a suspend function, resolves to Success or Failure.
            // If it could return Loading/Initial as terminal states, handle them or consider it an unexpected state.
            is CustomResult.Loading, is CustomResult.Initial, is CustomResult.Progress  -> return CustomResult.Failure(Exception("User session check in progress or uninitialized"))
        }

        // Fetch the user object using its ID.
        return when (val userFetchResult = userRepository.findById(userId)) {
            is CustomResult.Success -> {
                val user = userFetchResult.data
                
                // Call the domain logic on the User object to remove the profile image.
                // This method modifies the 'user' object in place and handles domain events.
                // It also includes checks for account status (e.g., withdrawn) and no-op changes.
                user.changeProfileImage(null)

                // Save the modified User object through the repository.
                // The save method handles persisting changes and publishing domain events.
                userRepository.save(user)
            }
            is CustomResult.Failure -> {
                // Return the failure result from fetching the user.
                CustomResult.Failure(userFetchResult.error)
            }
            // Assuming findById, as a suspend function, resolves to Success or Failure.
            // Handling Loading/Initial as terminal states implies an unexpected behavior from findById.
            is CustomResult.Loading, is CustomResult.Initial, is CustomResult.Progress -> {
                CustomResult.Failure(Exception("Failed to retrieve user information: repository in unexpected state"))
            }
        }
    }
}