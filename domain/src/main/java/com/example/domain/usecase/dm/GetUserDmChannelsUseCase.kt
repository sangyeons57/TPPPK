package com.example.domain.usecase.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.model.base.DMWrapper
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.DMChannelRepository
import com.example.domain.repository.DMWrapperRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 현재 로그인한 사용자의 DM 채널 목록을 스트림으로 가져오는 UseCase.
 * DMWrappers를 먼저 가져온 후, 각 wrapper에 해당하는 DMChannel 정보를 가져옵니다.
 */
class GetUserDmChannelsUseCase @Inject constructor(
    private val dmChannelRepository: DMChannelRepository,
    private val authRepository: AuthRepository,
    private val dmWrapperRepository: DMWrapperRepository
) {
    /**
     * 현재 로그인한 사용자의 모든 DM 채널을 Flow로 반환합니다.
     *
     * @return DM 채널 목록(List<DMChannel>)을 포함하는 CustomResult를 발행하는 Flow.
     *         진행 상태에 따라 Loading, Success, Failure를 발행할 수 있습니다.
     */
    operator fun invoke(): Flow<CustomResult<List<DMChannel>, Exception>> = flow {
        emit(CustomResult.Loading)

        // 1. Get current user session
        val currentUserSessionResult = authRepository.getCurrentUserSession()
        if (currentUserSessionResult !is CustomResult.Success) {
            val error = (currentUserSessionResult as? CustomResult.Failure)?.error
                ?: Exception("User not authenticated or session could not be retrieved.")
            emit(CustomResult.Failure(error))
            return@flow
        }
        val session = currentUserSessionResult.data
        // val currentUserId = currentUserSessionResult.data.userId // Not directly used if DMWrapperRepository handles current user

        // 2. Get DMWrappers for the current user
        // Assuming getDmWrappersStreamForCurrentUser() exists and is properly scoped or uses current user context
        dmWrapperRepository.getDMWrappersStream(session.userId).collect { dmWrappersResult ->
            when (dmWrappersResult) {
                is CustomResult.Loading -> {
                    // Initial loading already emitted. Can emit specific loading if needed.
                }

                is CustomResult.Success -> {
                    val wrappers = dmWrappersResult.data
                    if (wrappers.isEmpty()) {
                        emit(CustomResult.Success(emptyList()))
                    } else {
                        try {
                            // Fetch all DMChannels concurrently
                            val dmChannelDetailedResults = coroutineScope {
                                wrappers.map { wrapper ->
                                    async { dmChannelRepository.getDmChannelById(wrapper.dmChannelId.value) }
                                }.awaitAll()
                            }

                            val successfulChannels = mutableListOf<DMChannel>()
                            var firstError: Exception? = null

                            for (result in dmChannelDetailedResults) {
                                when (result) {
                                    is CustomResult.Success -> successfulChannels.add(result.data)
                                    is CustomResult.Failure -> {
                                        firstError = result.error
                                        break // Stop processing if one channel fails, and report this firstError
                                    }

                                    is CustomResult.Loading -> {
                                        firstError =
                                            Exception("getDmChannelById returned Loading"); break
                                    }

                                    is CustomResult.Initial -> {
                                        firstError =
                                            Exception("getDmChannelById returned Initial"); break
                                    }

                                    is CustomResult.Progress -> { /* TODO */
                                    }
                                }
                            }

                            if (firstError != null) {
                                emit(
                                    CustomResult.Failure(
                                        Exception(
                                            "Failed to load one or more DM channels.",
                                            firstError
                                        )
                                    )
                                )
                            } else {
                                emit(CustomResult.Success(successfulChannels.distinctBy { it.id })) // Ensure distinct channels
                            }
                        } catch (e: Exception) {
                            // Catch exceptions from coroutineScope or awaitAll
                            emit(
                                CustomResult.Failure(
                                    Exception(
                                        "Error processing DM channel details.",
                                        e
                                    )
                                )
                            )
                        }
                    }
                }

                is CustomResult.Failure -> {
                    emit(
                        CustomResult.Failure(
                            Exception(
                                "Failed to get DM wrappers.",
                                dmWrappersResult.error
                            )
                        )
                    )
                }

                is CustomResult.Initial -> { /* Optional: Handle Initial state from DMWrapper stream */
                }

                is CustomResult.Progress -> { /* Optional: Handle Progress state from DMWrapper stream */
                }
            }
        }
    }
}