package com.example.domain.usecase.user


import com.example.core_common.result.CustomResult
import com.example.core_common.result.CustomResult.Loading.getOrDefault
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject


/**
 * 사용자 닉네임을 업데이트하는 UseCase
 * 
 * @property userRepository 사용자 정보 관련 기능을 제공하는 Repository
 */
class UpdateNameUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    
    companion object {
        private const val MIN_NICKNAME_LENGTH = 3
        private const val MAX_NICKNAME_LENGTH = 20
        private val ALLOWED_NICKNAME_REGEX = "^[a-zA-Z0-9]*$".toRegex() // Alphanumeric
    }
    /**
     * 사용자의 닉네임을 업데이트합니다.
     * 닉네임은 공백을 제거하고, 길이 및 특수문자 등 유효성을 검사합니다.
     *
     * @param username 변경할 새 닉네임
     * @return 성공 시 성공 결과가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(username: UserName): CustomResult<User, Exception> {
        val trimmedNickname = username.trim()

        // 1. Validate nickname format
        if (trimmedNickname.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("닉네임은 비어있을 수 없습니다."))
        }
        if (trimmedNickname.length < MIN_NICKNAME_LENGTH) {
            return CustomResult.Failure(IllegalArgumentException("닉네임이 너무 짧습니다. (최소 ${MIN_NICKNAME_LENGTH}자)"))
        }
        if (trimmedNickname.length > MAX_NICKNAME_LENGTH) {
            return CustomResult.Failure(IllegalArgumentException("닉네임이 너무 깁니다. (최대 ${MAX_NICKNAME_LENGTH}자)"))
        }
        if (!trimmedNickname.matches(ALLOWED_NICKNAME_REGEX)) {
            return CustomResult.Failure(IllegalArgumentException("닉네임에 허용되지 않는 문자가 포함되어 있습니다."))
        }

        // 2. Check nickname availability
        try {
            when (val result = userRepository.observeByName(name = trimmedNickname).first()) {
                is CustomResult.Success -> {
                    // If a user is found, the nickname is already taken.
                    return CustomResult.Failure(IllegalArgumentException("이미 사용 중인 닉네임입니다."))
                }
                is CustomResult.Failure -> {
                    // If the specific error is 'NoSuchElementException', it means no user was found, so nickname IS available.
                    if (result.error !is NoSuchElementException) {
                        // Other errors from the repository call.
                        return CustomResult.Failure(Exception("닉네임 중복 확인 중 오류가 발생했습니다: ${result.error.localizedMessage}"))
                    }
                    // Nickname is available, continue
                }
                else -> {
                    return CustomResult.Failure(Exception("닉네임 중복 확인 중 오류가 발생했습니다."))
                }
            }
        } catch (e: Exception) {
            if (e !is NoSuchElementException) {
                return CustomResult.Failure(Exception("닉네임 검증 중 예상치 못한 오류가 발생했습니다: ${e.localizedMessage}"))
            }
            // Nickname is available, continue
        }

        // 3. Get current user session
        val session = authRepository.getCurrentUserSession().getOrDefault(null)
            ?: return CustomResult.Failure(Exception("User not logged in"))

        // 4. Fetch user and update name
        val user = when (val userResult = userRepository.findById(DocumentId.from(session.userId))) {
            is CustomResult.Success -> userResult.data as User
            is CustomResult.Failure -> return CustomResult.Failure(userResult.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(userResult.progress)
        }

        user.changeName(trimmedNickname)
        return when ( val userResult = userRepository.save(user)){
            is CustomResult.Success -> {
                EventDispatcher.publish(user)
                CustomResult.Success(user)
            }
            is CustomResult.Failure -> CustomResult.Failure(userResult.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(userResult.progress)

        }
    }
}