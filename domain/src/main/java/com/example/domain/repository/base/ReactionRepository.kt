package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Reaction
import com.example.domain.repository.DefaultRepository
import kotlinx.coroutines.flow.Flow

interface ReactionRepository : DefaultRepository {
    /**
     * 특정 메시지에 달린 모든 리액션 목록을 스트림으로 가져옵니다.
     */
    fun getReactionsStream(messageId: String): Flow<CustomResult<List<Reaction>, Exception>>
}
