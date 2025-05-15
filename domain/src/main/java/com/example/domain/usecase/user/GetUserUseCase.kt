package com.example.domain.usecase.user

import com.example.domain.model.User
import com.example.domain.model.UserProfileData
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 현재 로그인된 사용자의 정보를 가져오는 유스케이스 인터페이스
 */
interface GetUserUseCase {
    suspend operator fun invoke(): Result<UserProfileData>
}

/**
 * GetUserUseCase의 구현체
 * @param userRepository 사용자 데이터 접근을 위한 Repository
 * @param authRepository 현재 사용자 ID를 얻기 위한 Repository
 */
class GetUserUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : GetUserUseCase {

    /**
     * 유스케이스를 실행하여 현재 로그인된 사용자 정보를 가져옵니다.
     * AuthRepository를 통해 현재 사용자 ID를 얻고, UserRepository를 통해 User 정보를 가져온 후
     * UserProfileData로 변환하여 반환합니다.
     * @return Result<UserProfileData> 사용자 정보 로드 결과 (성공 시 UserProfileData, 실패 시 Exception)
     */
    override suspend fun invoke(): Result<UserProfileData> {
        // Get current user ID
        val currentUserId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not logged in")) // Return failure if no user ID

        // Fetch user info using the ID
        return userRepository.getUser(currentUserId).map { user ->
            user.toUserProfileData()
        }
    }

    /**
     * User(Domain Model) -> UserProfileData(UI Model) 변환 확장 함수
     * ProfileViewModel에 있던 변환 로직을 UseCase로 이동.
     * TODO: statusMessage는 User 모델에 없으므로 임시 처리. 추후 별도 로드 로직 필요.
     */
    private fun User.toUserProfileData(): UserProfileData {
        val tempStatusMessage = "상태 메시지 없음" // 임시 값
        return UserProfileData(
            userId = this.id,
            name = this.name,
            email = this.email,
            statusMessage = tempStatusMessage, // User 모델에 없는 정보 처리
            profileImageUrl = this.profileImageUrl
        )
    }
} 