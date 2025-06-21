package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserName
import com.example.domain.model.vo.user.UserMemo
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import javax.inject.Inject

/**
 * 사용자 프로필 정보를 업데이트하는 UseCase
 * 
 * @property userRepository 사용자 정보 관련 기능을 제공하는 Repository
 * @property authRepository 인증 관련 기능을 제공하는 Repository
 */
class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 사용자 프로필 정보를 업데이트합니다.
     *
     * @param params 업데이트할 사용자 정보 매개변수
     * @return 성공 시 업데이트된 User 객체, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(params: UpdateUserProfileParams): CustomResult<User, Exception> {
        val sessionResult = authRepository.getCurrentUserSession()
        return when (sessionResult) {
            is CustomResult.Success -> {
                val userRes = userRepository.findById(sessionResult.data.userId)
                if (userRes is CustomResult.Failure) {
                    return CustomResult.Failure(userRes.error)
                } else if (userRes !is CustomResult.Success) {
                    return  CustomResult.Failure(Exception("User not found"))
                }
                val user = userRes.data
                user.changeName(UserName(params.name))
                user.changeProfileImage(ImageUrl(params.profileImageUrl!!))
                params.memo?.let { user.changeMemo(UserMemo(it)) }
                when (val saveRes = userRepository.save(user)) {
                    is CustomResult.Success -> CustomResult.Success(user)
                    is CustomResult.Failure -> CustomResult.Failure(saveRes.error)
                    else -> CustomResult.Failure(Exception("Unknown error"))
                }
            }
            else -> CustomResult.Failure(Exception("Failed to get current user session"))
        }
    }
    
    /**
     * 사용자 프로필 정보를 업데이트합니다.
     * 
     * @param user 업데이트할 사용자 정보
     * @return 성공 시 업데이트된 User 객체, 실패 시 에러 정보가 포함된 Result
     */
    @Deprecated("Use behaviour-based overload")
    suspend operator fun invoke(user: User): CustomResult<String, Exception> {
        val sessionResult = authRepository.getCurrentUserSession()
        return when (sessionResult) {
            is CustomResult.Success -> userRepository.save(user)
            else -> CustomResult.Failure(Exception("Failed to get current user session"))
        }
    }
}
