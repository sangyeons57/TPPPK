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
 * DM 채널 차단을 해제하는 UseCase입니다.
 * Firebase Function을 통해 채널의 차단을 해제하고 필요시 DMWrapper를 재생성합니다.
 * 작업 진행 상태(Loading, Success, Failure)를 Flow를 통해 전달합니다.
 */
class UnblockDMChannelUseCase @Inject constructor(
    private val dmChannelRepository: DMChannelRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 지정된 DM 채널의 차단을 해제합니다.
     *
     * @param channelId 차단 해제할 DM 채널의 ID
     * @return 채널 차단 해제 과정을 나타내는 Flow. CustomResult.Loading, CustomResult.Success(channelId),
     *         또는 CustomResult.Failure(exception)를 순차적으로 발행합니다.
     *         성공 시 추가 정보로 완전히 해제되었는지 여부를 포함합니다.
     */
    operator fun invoke(channelId: DocumentId): Flow<CustomResult<Map<String, Any?>, Exception>> = flow {
        emit(CustomResult.Loading)

        if (channelId.isBlank()) {
            emit(CustomResult.Failure(IllegalArgumentException("Channel ID cannot be blank.")))
            return@flow
        }

        val session = authRepository.getCurrentUserSession()
        when (session) {
            is CustomResult.Success -> {
                // 인증된 사용자가 있음, Firebase Function 호출
                val unblockResult = dmChannelRepository.unblockDMChannel(channelId.value)
                when (unblockResult) {
                    is CustomResult.Success -> {
                        // Firebase Function으로부터 받은 결과 확인
                        val resultData = unblockResult.data
                        val success = resultData["success"] as? Boolean ?: false
                        
                        if (success) {
                            emit(CustomResult.Success(resultData))
                        } else {
                            val message = resultData["message"] as? String ?: "Failed to unblock DM channel"
                            emit(CustomResult.Failure(Exception(message)))
                        }
                    }
                    is CustomResult.Failure -> {
                        emit(CustomResult.Failure(unblockResult.error))
                    }
                    else -> {
                        emit(CustomResult.Failure(Exception("Unexpected result type from unblockDMChannel")))
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
        emit(CustomResult.Failure(Exception("An unexpected error occurred in UnblockDMChannelUseCase: ${e.message}", e)))
    }
}