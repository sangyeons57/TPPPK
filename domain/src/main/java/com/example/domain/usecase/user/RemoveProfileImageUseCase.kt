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
        // The profile image removal flow has been deprecated after we switched to fixed
        // storage paths that are resolved client-side. Returning a failure until a new
        // behaviour is defined.
        return CustomResult.Failure(Exception("RemoveProfileImageUseCase is deprecated"))
    }
}