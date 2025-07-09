package com.example.domain.usecase.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.DMChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * DM 채널을 차단하는 UseCase입니다.
 * Firebase Function을 통해 채널을 차단하고 DMWrapper를 제거합니다.
 * 작업 진행 상태(Loading, Success, Failure)를 Flow를 통해 전달합니다.
 */
class BlockDMChannelUseCase @Inject constructor(
    private val dmChannelRepository: DMChannelRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 지정된 DM 채널을 차단합니다.
     *
     * @param channelId 차단할 DM 채널의 ID
     * @return 채널 차단 과정을 나타내는 Flow. CustomResult.Loading, CustomResult.Success(channelId),
     *         또는 CustomResult.Failure(exception)를 순차적으로 발행합니다.
     *         실패 사유는 채널 ID가 비어있거나, 인증되지 않았거나, 채널을 찾을 수 없거나,
     *         이미 차단된 채널이거나, 권한이 없거나, 차단 중 오류가 발생한 경우 등입니다.
     */
    operator fun invoke(channelId: DocumentId): Flow<CustomResult<DocumentId, Exception>> = flow {
        emit(CustomResult.Loading)

        if (channelId.isBlank()) {
            emit(CustomResult.Failure(IllegalArgumentException("Channel ID cannot be blank.")))
            return@flow
        }

        val session = authRepository.getCurrentUserSession()
        when (session) {
            is CustomResult.Success -> {
                // 인증된 사용자가 있음, Firebase Function 호출
                val blockResult = dmChannelRepository.blockDMChannel(channelId.value)
                when (blockResult) {
                    is CustomResult.Success -> {
                        // Firebase Function으로부터 받은 결과 확인
                        val resultData = blockResult.data
                        val success = resultData["success"] as? Boolean ?: false
                        
                        if (success) {
                            emit(CustomResult.Success(channelId))
                        } else {
                            val message = resultData["message"] as? String ?: "Failed to block DM channel"
                            emit(CustomResult.Failure(Exception(message)))
                        }
                    }
                    is CustomResult.Failure -> {
                        emit(CustomResult.Failure(blockResult.error))
                    }
                    else -> {
                        emit(CustomResult.Failure(Exception("Unexpected result type from blockDMChannel")))
                    }
                }
            }
            is CustomResult.Failure -> {
                emit(CustomResult.Failure(Exception("User not logged in: ${session.error.message}")))
            }
            else -> {
                emit(CustomResult.Failure(Exception("Unable to get user session")))
            }
        }
    }.catch { e ->
        emit(CustomResult.Failure(Exception("An unexpected error occurred in BlockDMChannelUseCase: ${e.message}", e)))
    }
}