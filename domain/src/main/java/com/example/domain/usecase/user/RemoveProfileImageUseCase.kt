package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
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
        TODO("not implemented yet [Firebase 에서 구현 해야함 제거된거 다른곳에서 적용해야함]")
        val userId = when (val sessionResult = authRepository.getCurrentUserSession()) {
            is CustomResult.Success -> sessionResult.data.userId
            is CustomResult.Failure -> return CustomResult.Failure(sessionResult.error ?: Exception("User not logged in"))
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(sessionResult.progress)
        }

        // Fetch the user object using its ID.
        val user = when (val userFetchResult = userRepository.findById(DocumentId.from(userId))) {
            is CustomResult.Success -> userFetchResult.data  as User
            is CustomResult.Failure -> return CustomResult.Failure(userFetchResult.error)
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Progress -> return CustomResult.Progress(userFetchResult.progress)
        }
        user.removeProfileImage()
        return userRepository.save(user).suspendSuccessProcess {
            EventDispatcher.publish(user)
            CustomResult.Success(it.value)
        }
    }
}