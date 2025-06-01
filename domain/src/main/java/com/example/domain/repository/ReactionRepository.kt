package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Reaction
import kotlinx.coroutines.flow.Flow

interface ReactionRepository {
    /**
     * 특정 메시지에 달린 모든 리액션 목록을 스트림으로 가져옵니다.
     */
    fun getReactionsStream(messageId: String): Flow<CustomResult<List<Reaction>, Exception>>

    /**
     * 메시지에 리액션을 추가합니다.
     * 성공 시 추가된 리액션 정보를 반환할 수 있습니다.
     * @param messageId 리액션을 추가할 메시지 ID
     * @param userId 리액션을 추가하는 사용자 ID
     * @param emoji 표현할 이모지 (String 또는 특정 ID)
     */
    suspend fun addReaction(messageId: String, userId: String, emoji: String): CustomResult<Reaction, Exception>

    /**
     * 메시지에서 특정 리액션을 삭제합니다.
     * @param reactionId 삭제할 리액션의 고유 ID
     * (또는 messageId, userId, emoji 조합으로 삭제할 수도 있음)
     */
    suspend fun removeReaction(reactionId: String): CustomResult<Unit, Exception>

    // TODO: 사용자가 특정 메시지에 이미 특정 이모지로 리액션했는지 확인하는 함수 등 필요시 추가
}
