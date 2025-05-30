package com.example.data.repository

import com.example.core_common.result.CustomResult
// import com.example.data.datasource.remote.ReactionRemoteDataSource // TODO: DataSource 생성 후 주석 해제
import com.example.data.model.mapper.toDomain // TODO: 실제 매퍼 경로 및 함수 확인
import com.example.data.model.mapper.toDto // TODO: 실제 매퍼 경로 및 함수 확인
import com.example.domain.model.base.Reaction
import com.example.domain.repository.ReactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf // 임시
import javax.inject.Inject

class ReactionRepositoryImpl @Inject constructor(
    // private val reactionRemoteDataSource: ReactionRemoteDataSource // TODO: DataSource 생성 후 주석 해제
    // TODO: 필요한 Mapper 주입
) : ReactionRepository {

    override fun getReactionsStream(messageId: String): Flow<CustomResult<List<Reaction>>> {
        // TODO: reactionRemoteDataSource.getReactionsStream(messageId).map { ... } 구현
        throw NotImplementedError("ReactionRemoteDataSource 필요 및 구현 필요")
        // return flowOf(CustomResult.Success(emptyList())) // 임시 반환
    }

    override suspend fun addReaction(messageId: String, userId: String, emoji: String): CustomResult<Reaction> {
        // val reactionDto = Reaction(id = "", messageId = messageId, userId = userId, emoji = emoji, createdAt = java.time.LocalDateTime.now()).toDto() // 예시
        // TODO: reactionRemoteDataSource.addReaction(...) 구현
        throw NotImplementedError("ReactionRemoteDataSource 필요 및 구현 필요")
        // return CustomResult.Error(NotImplementedError("...")) // 임시 반환
    }

    override suspend fun removeReaction(reactionId: String): CustomResult<Unit> {
        // TODO: reactionRemoteDataSource.removeReaction(reactionId) 구현
        throw NotImplementedError("ReactionRemoteDataSource 필요 및 구현 필요")
        // return CustomResult.Error(NotImplementedError("...")) // 임시 반환
    }
}
