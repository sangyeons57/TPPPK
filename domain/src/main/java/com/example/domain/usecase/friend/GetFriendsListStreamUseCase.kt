package com.example.domain.usecase.friend

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Friend
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.FriendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 친구 목록 스트림을 가져오는 UseCase
 *
 * @property friendRepository 친구 관련 기능을 제공하는 Repository
 */
class GetFriendsListStreamUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 친구 목록 정보를 실시간 스트림으로 가져옵니다.
     * Repository는 이미 특정 사용자의 컨텍스트로 생성되었으므로 별도의 userId가 필요하지 않습니다.
     *
     * @return Flow<CustomResult<List<Friend>, Exception>> 친구 목록 정보 Flow
     */
    operator fun invoke(): Flow<CustomResult<List<Friend>, Exception>> {
        val session = when (val result = authRepository.getCurrentUserSession()) {
            is CustomResult.Success -> result.data
            is CustomResult.Failure -> return flowOf(CustomResult.Failure(Exception("로그인 상태를 확인할 수 없습니다.")))
            is CustomResult.Loading -> return flowOf(CustomResult.Loading)
            is CustomResult.Initial -> return flowOf(CustomResult.Initial)
            is CustomResult.Progress -> return flowOf(CustomResult.Progress(result.progress))
        }

        return friendRepository.observeFriendsList(session.userId.value)
            .map { result ->
                when (result) {
                    is CustomResult.Success -> CustomResult.Success(result.data)
                    is CustomResult.Failure -> CustomResult.Failure(Exception("친구 목록을 가져오는 데 실패했습니다."))
                    is CustomResult.Loading -> CustomResult.Loading
                    is CustomResult.Initial -> CustomResult.Initial
                    is CustomResult.Progress -> CustomResult.Progress(result.progress)
                }
            }
    }
}