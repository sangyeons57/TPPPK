package com.example.domain.usecase.dm

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.DMChannelRepository
import com.example.domain.repository.base.DMWrapperRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

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
        Log.d("GetUserDmChannelsUseCase", "Flow started → emitting Loading")
        emit(CustomResult.Loading)

        // 1. Get current user session
        val currentUserSessionResult = authRepository.getCurrentUserSession().also {
            Log.d("GetUserDmChannelsUseCase", "getCurrentUserSession() result = $it")
        }
        if (currentUserSessionResult !is CustomResult.Success) {
            val error = (currentUserSessionResult as? CustomResult.Failure)?.error
                ?: Exception("User not authenticated or session could not be retrieved.")
            emit(CustomResult.Failure(error))
            return@flow
        }
        // val currentUserId = currentUserSessionResult.data.userId // Not directly used if DMWrapperRepository handles current user

        // 2. Get DMWrappers for the current user
        // Assuming getDmWrappersStreamForCurrentUser() exists and is properly scoped or uses current user context
        Log.d("GetUserDmChannelsUseCase", "Start collecting dmWrapperRepository.observeAll()")
        dmWrapperRepository.observeAll().collect { dmWrappersResult ->
            Log.d("GetUserDmChannelsUseCase", "dmWrapperRepository.observeAll() emitted: $dmWrappersResult")
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
                                    async {
                                        Log.d("GetUserDmChannelsUseCase", "Fetching DMChannel id=${wrapper.id}")
                                        dmChannelRepository.findById(wrapper.id)
                                    }
                                }.awaitAll()
                            }

                            val successfulChannels :MutableList<DMChannel> = mutableListOf<DMChannel>()
                            var firstError: Exception? = null

                            for (result in dmChannelDetailedResults) {
                                when (result) {
                                    is CustomResult.Success -> successfulChannels.add((result.data)  as DMChannel)
                                    is CustomResult.Failure -> {
                                        Log.e("GetUserDmChannelsUseCase", "Failed to fetch DMChannel: ${result.error}")
                                        firstError = result.error
                                        break // Stop processing if one channel fails, and report this firstError
                                    }

                                    is CustomResult.Loading -> {
                                        Log.d("GetUserDmChannelsUseCase", "Channel fetch returned Loading state")
                                        firstError =
                                            Exception("getDmChannelById returned Loading"); break
                                    }

                                    is CustomResult.Initial -> {
                                        Log.d("GetUserDmChannelsUseCase", "Channel fetch returned Initial state")
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
                    // 권한 에러인 경우 특별 처리: 빈 리스트 반환
                    val isPermissionError = dmWrappersResult.error is FirebaseFirestoreException &&
                            (dmWrappersResult.error as FirebaseFirestoreException).code == FirebaseFirestoreException.Code.PERMISSION_DENIED

                    if (isPermissionError) {
                        Log.w("GetUserDmChannelsUseCase", "Permission denied for DM wrappers - returning empty list (user likely logged out)")
                        emit(CustomResult.Success(emptyList()))
                    } else {
                        emit(
                            CustomResult.Failure(
                                Exception(
                                    "Failed to get DM wrappers.",
                                    dmWrappersResult.error
                                )
                            )
                        )
                    }
                }

                is CustomResult.Initial -> { /* Optional: Handle Initial state from DMWrapper stream */
                }

                is CustomResult.Progress -> { /* Optional: Handle Progress state from DMWrapper stream */
                }
            }
        }
    }.catch { exception ->
        // 전체 Flow에서 발생하는 예외를 catch하여 graceful하게 처리
        Log.e("GetUserDmChannelsUseCase", "Unexpected error in flow", exception)
        
        // 권한 에러인 경우 빈 리스트 반환
        val isPermissionError = exception is FirebaseFirestoreException &&
                exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        
        if (isPermissionError) {
            Log.w("GetUserDmChannelsUseCase", "Permission denied - returning empty list (user likely logged out)")
            emit(CustomResult.Success(emptyList()))
        } else {
            emit(CustomResult.Failure(Exception("Unexpected error occurred while loading DM channels", exception)))
        }
    }
}