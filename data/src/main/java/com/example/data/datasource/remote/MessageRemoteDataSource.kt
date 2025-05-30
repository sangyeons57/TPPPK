
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.model.remote.MessageDTO
import com.example.data.model.remote.ReactionDTO
import kotlinx.coroutines.flow.Flow

interface MessageRemoteDataSource {

    /**
     * 특정 채널의 메시지 목록을 실시간으로 관찰합니다.
     * @param channelPath 메시지를 가져올 채널의 전체 경로 (예: "dm_channels/channelId123")
     * @param limit 가져올 메시지의 개수
     */
    fun observeMessages(channelPath: String, limit: Long): Flow<List<MessageDTO>>

    /**
     * 특정 채널의 특정 메시지 정보를 한 번 가져옵니다.
     * @param channelPath 메시지를 가져올 채널의 전체 경로
     * @param messageId 가져올 메시지의 ID
     */
    suspend fun getMessage(channelPath: String, messageId: String): CustomResult<MessageDTO?, Exception>

    /**
     * 특정 채널에 메시지를 전송합니다.
     * @param channelPath 메시지를 보낼 채널의 전체 경로
     * @param content 보낼 메시지의 내용
     * @return 생성된 메시지의 ID를 포함한 Result 객체
     */
    suspend fun sendMessage(channelPath: String, content: String): CustomResult<String, Exception>

    /**
     * 메시지 내용을 수정합니다.
     * @param channelPath 대상 채널의 전체 경로
     * @param messageId 수정할 메시지의 ID
     * @param newContent 새로운 메시지 내용
     */
    suspend fun updateMessage(channelPath: String, messageId: String, newContent: String): CustomResult<Unit, Exception>

    /**
     * 메시지를 삭제(soft delete)합니다.
     * @param channelPath 대상 채널의 전체 경로
     * @param messageId 삭제할 메시지의 ID
     */
    suspend fun deleteMessage(channelPath: String, messageId: String): CustomResult<Unit, Exception>

    /**
     * 특정 메시지의 리액션 목록을 실시간으로 관찰합니다.
     * @param channelPath 대상 채널의 전체 경로
     * @param messageId 리액션을 가져올 메시지의 ID
     */
    fun observeReactions(channelPath: String, messageId: String): Flow<List<ReactionDTO>>

    /**
     * 메시지에 리액션을 추가합니다.
     * @param channelPath 대상 채널의 전체 경로
     * @param messageId 리액션을 추가할 메시지의 ID
     * @param emoji 추가할 이모지
     */
    suspend fun addReaction(channelPath: String, messageId: String, emoji: String): CustomResult<Unit, Exception>

    /**
     * 메시지에서 리액션을 제거합니다.
     * @param channelPath 대상 채널의 전체 경로
     * @param messageId 리액션을 제거할 메시지의 ID
     * @param emoji 제거할 이모지
     */
    suspend fun removeReaction(channelPath: String, messageId: String, emoji: String): CustomResult<Unit, Exception>
}

