package com.example.data_repository.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain_repository.local.LocalMessageRepository
import java.time.Instant
import javax.inject.Inject

/**
 * 양방향 페이징을 지원하는 RemoteMediator
 * 중간 진입점에서 위/아래 양방향으로 메시지를 로드할 수 있습니다.
 */
@OptIn(ExperimentalPagingApi::class)
class BidirectionalPagingMediator @Inject constructor(
    private val localMessageRepository: LocalMessageRepository,
    private val anchorMessageId: String? = null
) : RemoteMediator<Long, Message>() {

    private var anchorTimestamp: Long? = null
    private var isInitialized = false

    override suspend fun initialize(): InitializeAction {
        return if (isInitialized) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Long, Message>
    ): MediatorResult {
        return try {
            when (loadType) {
                LoadType.REFRESH -> {
                    loadAnchorData(state)
                }

                LoadType.PREPEND -> {
                    loadNewerMessages(state)
                }

                LoadType.APPEND -> {
                    loadOlderMessages(state)
                }
            }
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun loadAnchorData(state: PagingState<Long, Message>): MediatorResult {
        // 초기 로드 또는 새로고침
        if (!isInitialized && anchorMessageId != null) {
            // 중간 진입점에서 시작하는 경우
            val result = loadMessagesAroundAnchor(anchorMessageId)
            return when (result) {
                is CustomResult.Success -> {
                    val messages = result.data
                    if (messages.isNotEmpty()) {
                        // 기준점 타임스탬프 설정
                        anchorTimestamp =
                            messages.find { it.id.value == anchorMessageId }?.createdAt?.toEpochMilli()
                    }
                    isInitialized = true
                    MediatorResult.Success(endOfPaginationReached = false)
                }

                is CustomResult.Failure -> {
                    MediatorResult.Error(result.error)
                }

                else -> {
                    MediatorResult.Error(Exception("Unknown result type"))
                }
            }
        } else {
            // 일반적인 최신 메시지부터 시작
            val result = localMessageRepository.getMessagesBefore(
                beforeTimestamp = Instant.now().toEpochMilli(),
                limit = state.config.pageSize
            )
            return when (result) {
                is CustomResult.Success -> {
                    isInitialized = true
                    MediatorResult.Success(endOfPaginationReached = result.data.size < state.config.pageSize)
                }

                is CustomResult.Failure -> {
                    MediatorResult.Error(result.error)
                }

                else -> {
                    MediatorResult.Error(Exception("Unknown result type"))
                }
            }
        }
    }

    private suspend fun loadNewerMessages(state: PagingState<Long, Message>): MediatorResult {
        // 더 최신 메시지 로드 (위쪽 방향)
        val timestamp = anchorTimestamp ?: run {
            val firstMessage = state.firstItemOrNull()
            firstMessage?.createdAt?.toEpochMilli() ?: Instant.now().toEpochMilli()
        }

        val result = localMessageRepository.getMessagesAfter(
            afterTimestamp = timestamp,
            limit = state.config.pageSize
        )

        return when (result) {
            is CustomResult.Success -> {
                MediatorResult.Success(endOfPaginationReached = result.data.size < state.config.pageSize)
            }

            is CustomResult.Failure -> {
                MediatorResult.Error(result.error)
            }

            else -> {
                MediatorResult.Error(Exception("Unknown result type"))
            }
        }
    }

    private suspend fun loadOlderMessages(state: PagingState<Long, Message>): MediatorResult {
        // 더 과거 메시지 로드 (아래쪽 방향)
        val timestamp = anchorTimestamp ?: run {
            val lastMessage = state.lastItemOrNull()
            lastMessage?.createdAt?.toEpochMilli() ?: 0L
        }

        val result = localMessageRepository.getMessagesBefore(
            beforeTimestamp = timestamp,
            limit = state.config.pageSize
        )

        return when (result) {
            is CustomResult.Success -> {
                MediatorResult.Success(endOfPaginationReached = result.data.size < state.config.pageSize)
            }

            is CustomResult.Failure -> {
                MediatorResult.Error(result.error)
            }

            else -> {
                MediatorResult.Error(Exception("Unknown result type"))
            }
        }
    }

    private suspend fun loadMessagesAroundAnchor(anchorMessageId: String): CustomResult<List<Message>, Exception> {
        // TODO: MessageDao의 getMessagesAroundAnchor를 사용하여 기준점 주변 메시지 로드
        // 현재는 기본 구현으로 대체
        return localMessageRepository.getMessagesBefore(
            beforeTimestamp = Instant.now().toEpochMilli(),
            limit = 50
        )
    }
}