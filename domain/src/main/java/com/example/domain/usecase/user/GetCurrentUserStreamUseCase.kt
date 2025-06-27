package com.example.domain.usecase.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.UserRepository
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 현재 사용자의 정보를 실시간 스트림으로 조회하는 UseCase 인터페이스입니다.
 */
interface GetCurrentUserStreamUseCase {
    /**
     * 현재 로그인된 사용자의 정보를 실시간 Flow로 반환합니다.
     * @return 사용자 정보 Flow
     */
    suspend operator fun invoke(): Flow<CustomResult<User, Exception>>
}

/**
 * 현재 사용자의 정보를 실시간 스트림으로 조회하는 UseCase 구현체입니다.
 */
class GetCurrentUserStreamUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : GetCurrentUserStreamUseCase {
    /**
     * 현재 로그인된 사용자의 정보를 실시간 Flow로 반환합니다.
     * @return 사용자 정보 Flow
     */
    override suspend operator fun invoke(): Flow<CustomResult<User, Exception>> {
        return when (val session = authRepository.getCurrentUserSession()) {
            is CustomResult.Success -> {
                userRepository.observe(DocumentId.from(session.data.userId)).map { result ->
                    when (result) {
                        is CustomResult.Success -> CustomResult.Success(result.data as User)
                        is CustomResult.Failure -> CustomResult.Failure(result.error)
                        is CustomResult.Initial -> CustomResult.Initial
                        is CustomResult.Loading -> CustomResult.Loading
                        is CustomResult.Progress -> CustomResult.Progress(result.progress)
                    }
                }
            }
            is CustomResult.Failure -> flowOf( CustomResult.Failure(session.error))
            is CustomResult.Initial -> flowOf( CustomResult.Initial )
            is CustomResult.Loading -> flowOf( CustomResult.Loading )
            is CustomResult.Progress -> flowOf( CustomResult.Progress(session.progress))
        }
    }
} 