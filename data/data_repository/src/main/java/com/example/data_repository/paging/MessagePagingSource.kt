package com.example.data_repository.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain_repository.local.LocalMessageRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Room 기반 Message Paging Source
 * 타임스탬프를 키로 사용하여 시간 순으로 메시지를 페이징합니다.
 */
class MessagePagingSource @Inject constructor(
    private val localMessageRepository: LocalMessageRepository
) : PagingSource<Long, Message>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Message> {
        return try {
            val beforeTimestamp = params.key ?: Instant.now().toEpochMilli()
            val pageSize = params.loadSize

            // Room에서 메시지 조회 (시간 역순)
            val result = localMessageRepository.getMessagesBefore(beforeTimestamp, pageSize)

            when (result) {
                is CustomResult.Success -> {
                    val messages = result.data

                    // 다음 페이지 키 결정 (가장 오래된 메시지의 타임스탬프)
                    val nextKey = if (messages.isNotEmpty() && messages.size == pageSize) {
                        messages.last().createdAt.toEpochMilli()
                    } else {
                        null // 더 이상 로드할 데이터가 없음
                    }

                    LoadResult.Page(
                        data = messages,
                        prevKey = null, // 단방향 페이징 (과거 방향만)
                        nextKey = nextKey
                    )
                }

                is CustomResult.Failure -> {
                    LoadResult.Error(result.error)
                }

                else -> {
                    LoadResult.Error(Exception("Unknown result type"))
                }
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, Message>): Long? {
        // 새로 고침 시 현재 시간을 키로 사용
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        } ?: Instant.now().toEpochMilli()
    }
}