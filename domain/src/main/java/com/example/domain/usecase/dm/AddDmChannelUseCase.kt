package com.example.domain.usecase.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.DMChannelRepository
import com.example.domain.repository.DMWrapperRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow // Flow import
import kotlinx.coroutines.flow.catch // catch import
import kotlinx.coroutines.flow.filter // filter import
import kotlinx.coroutines.flow.first // first import
import kotlinx.coroutines.flow.flow // flow builder import
import javax.inject.Inject

/**
 * 새로운 DM 채널을 생성하거나 기존 DM 채널을 가져오는 UseCase입니다.
 * 파트너의 사용자 이름을 기반으로 사용자를 조회한 후, 해당 사용자와의 DM 채널을 식별하거나 생성합니다.
 * 작업 진행 상태(Loading, Success, Failure)를 Flow를 통해 전달합니다.
 */
class AddDmChannelUseCase @Inject constructor(
    private val dmChannelRepository: DMChannelRepository,
    private val dmWrapperRepository: DMWrapperRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
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
    operator fun invoke(partnerName: String): Flow<CustomResult<String, Exception>> = flow {
        emit(CustomResult.Loading)

        if (partnerName.isBlank()) {
            emit(CustomResult.Failure(IllegalArgumentException("Partner name cannot be blank.")))
            return@flow
        }

        val session = when (val currentUserSessionResult = authRepository.getCurrentUserSession()) {
            is CustomResult.Success -> currentUserSessionResult.data
            is CustomResult.Failure -> {
                emit(CustomResult.Failure(
                    Exception("User not authenticated: ${currentUserSessionResult.error.message}", currentUserSessionResult.error)
                ))
                return@flow
            }
            else -> { // Loading, Initial 등 기타 상태 처리
                emit(CustomResult.Failure(Exception("User session status unknown or not yet loaded.")))
                return@flow
            }
        }

        val partnerUserResult: CustomResult<User, Exception> = userRepository.findByNameStream(partnerName)
            .filter { it !is CustomResult.Loading && it !is CustomResult.Initial } 
            .first()

        val partnerId = when (partnerUserResult) {
            is CustomResult.Success -> partnerUserResult.data.uid
            is CustomResult.Failure -> {
                emit(CustomResult.Failure(partnerUserResult.error))
                return@flow
            }
            is CustomResult.Loading, is CustomResult.Initial -> {
                emit(CustomResult.Failure(Exception("Failed to get partner user information.")))
                return@flow
            }
            else -> { // Loading, Initial 등 기타 상태 처리 (filter 후에는 Success/Failure만 남아야 함)
                emit(CustomResult.Failure(Exception("Failed to get partner user information after filtering.")))
                return@flow
            }
        }

        if (session.userId == partnerId.value) {
            emit(CustomResult.Failure(IllegalArgumentException("Cannot create DM channel with oneself.")))
            return@flow
        }

        // 5. DM 채널 생성 또는 가져오기 (DMChannelRepository)
        when (val dmChannelCreationResult = dmChannelRepository.createDmChannel(partnerId.value)) {
            is CustomResult.Success -> {
                val dmChannelId = dmChannelCreationResult.data
                // DMWrapper 생성 요청
                val dmWrapperCreationResult = dmWrapperRepository.createDMWrapper(
                    userId = session.userId,
                    dmChannelId = dmChannelId,
                    otherUserId = partnerId.value
                )

                // DMWrapper 생성 결과에 따라 최종 상태 emit
                when (dmWrapperCreationResult) {
                    is CustomResult.Success -> {
                        // DMWrapper 생성 성공 시, DM 채널 ID를 성공 결과로 emit
                        emit(CustomResult.Success(dmChannelId))
                    }
                    is CustomResult.Failure -> {
                        emit(CustomResult.Failure(
                            Exception("Failed to create DMWrapper: ${dmWrapperCreationResult.error.message}", dmWrapperCreationResult.error))
                        )
                    }
                    else ->{
                        emit(CustomResult.Failure(
                            Exception("Unknown error type from createDMWrapper.")
                        ))
                    }
                    // createDMWrapper는 suspend 함수이므로 Loading, Initial 상태를 직접 반환하지 않음
                }
            }
            is CustomResult.Failure -> {
                emit(CustomResult.Failure(
                    Exception("Failed to create DM channel: ${dmChannelCreationResult.error.message}", dmChannelCreationResult.error))
                )
            }
            else -> { // Loading, Initial 등 기타 상태 처리
                emit(CustomResult.Failure(Exception("Unknown error type from createDmChannel.")))
            }
        }

    }.catch { e ->
        emit(CustomResult.Failure(Exception("An unexpected error occurred in AddDmChannelUseCase: ${e.message}", e)))
    }
}
