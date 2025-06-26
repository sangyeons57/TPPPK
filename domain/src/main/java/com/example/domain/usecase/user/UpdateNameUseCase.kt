package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.User
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import javax.inject.Inject

/**
 * 사용자의 이름을 업데이트하는 UseCase
 * DDD 원칙에 따라 도메인 모델의 changeName 메서드를 사용하고 저장합니다.
 */
class UpdateNameUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    
    /**
     * 현재 사용자의 이름을 업데이트합니다.
     *
     * @param newName 새로운 사용자 이름
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend operator fun invoke(newName: UserName): CustomResult<User, Exception> {
        return try {
            // 1. 현재 사용자 조회
            val session: UserSession =
                when (val sessionResult = authRepository.getCurrentUserSession()) {
                    is CustomResult.Success -> sessionResult.data
                    is CustomResult.Failure -> return CustomResult.Failure(sessionResult.error)
                    is CustomResult.Initial -> return CustomResult.Initial
                    is CustomResult.Loading -> return CustomResult.Loading
                    is CustomResult.Progress -> return CustomResult.Progress(sessionResult.progress)
            }


            val user =
                when (val userResult = userRepository.findById(DocumentId.from(session.userId))) {
                    is CustomResult.Success -> userResult.data as User
                    is CustomResult.Failure -> return CustomResult.Failure(userResult.error)
                    is CustomResult.Initial -> return CustomResult.Initial
                    is CustomResult.Loading -> return CustomResult.Loading
                    is CustomResult.Progress -> return CustomResult.Progress(userResult.progress)
                }

            // 2. 도메인 모델에서 이름 변경 (비즈니스 로직 포함)
            user.changeName(newName)

            // 3. 변경된 사용자 저장
            when (val saveResult = userRepository.save(user)) {
                is CustomResult.Success -> {
                    EventDispatcher.publish(user)
                    CustomResult.Success(user)
                }

                is CustomResult.Failure -> CustomResult.Failure(saveResult.error)
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Progress -> CustomResult.Progress(saveResult.progress)
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
}