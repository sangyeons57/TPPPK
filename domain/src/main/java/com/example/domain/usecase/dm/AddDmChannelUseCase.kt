package com.example.domain.usecase.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.DMChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 새로운 DM 채널을 생성하거나 기존 DM 채널을 가져오는 UseCase입니다.
 * Firebase Function을 통해 사용자 이름을 기반으로 DM 채널을 생성합니다.
 * 작업 진행 상태(Loading, Success, Failure)를 Flow를 통해 전달합니다.
 */
class AddDmChannelUseCase @Inject constructor(
    private val dmChannelRepository: DMChannelRepository,
    private val authRepository: AuthRepository
) {
    /**
     * 지정된 상대방 사용자 이름으로 DM 채널을 생성하거나 가져옵니다.
     *
     * @param partnerName DM 채널을 생성할 상대방의 사용자 이름.
     * @return DM 채널 ID 생성 과정을 나타내는 Flow. CustomResult.Loading, CustomResult.Success(channelId),
     *         또는 CustomResult.Failure(exception)를 순차적으로 발행합니다.
     *         실패 사유는 파트너 이름이 비어있거나, 인증되지 않았거나, 파트너 사용자를 찾을 수 없거나,
     *         자기 자신과의 DM을 시도했거나, DM 채널 생성 중 오류가 발생한 경우 등입니다.
     */
    operator fun invoke(partnerName: UserName): Flow<CustomResult<DocumentId, Exception>> = flow {
        emit(CustomResult.Loading)

        if (partnerName.isBlank()) {
            emit(CustomResult.Failure(IllegalArgumentException("Partner name cannot be blank.")))
            return@flow
        }

        val session = authRepository.getCurrentUserSession()
        when (session) {
            is CustomResult.Success -> {
                // 인증된 사용자가 있음, Firebase Function 호출
                val createDMResult = dmChannelRepository.createDMChannel(partnerName.value)
                when (createDMResult) {
                    is CustomResult.Success -> {
                        // Firebase Function으로부터 받은 결과에서 channelId 추출
                        val resultData = createDMResult.data
                        val channelId = resultData["channelId"] as? String
                        
                        if (channelId != null) {
                            emit(CustomResult.Success(DocumentId.from(channelId)))
                        } else {
                            emit(CustomResult.Failure(Exception("Channel ID not found in response")))
                        }
                    }
                    is CustomResult.Failure -> {
                        // 차단된 사용자인지 확인
                        if (createDMResult.error.message?.contains("차단된 사용자입니다") == true) {
                            emit(CustomResult.Failure(Exception("차단된 사용자입니다.")))
                        } else {
                            emit(CustomResult.Failure(createDMResult.error))
                        }
                    }
                    else -> {
                        emit(CustomResult.Failure(Exception("Unexpected result type from createDMChannel")))
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
        emit(CustomResult.Failure(Exception("An unexpected error occurred in AddDmChannelUseCase: ${e.message}", e)))
    }
}
