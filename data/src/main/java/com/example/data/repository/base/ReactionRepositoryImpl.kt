package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.model.remote.ReactionDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.event.AggregateRoot
// import com.example.data.datasource.remote.ReactionRemoteDataSource // TODO: DataSource 생성 후 주석 해제
import com.example.domain.model.base.Reaction
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.base.ReactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 메시지 반응(이모지) 관리를 위한 저장소 구현체
 * 현재는 임시 구현으로, ReactionRemoteDataSource가 구현되면 실제 구현으로 교체 예정
 */
class ReactionRepositoryImpl @Inject constructor(
    private val reactionRemoteDataSource: ReactionRemoteDataSource // TODO: DataSource 생성 후 주석 해제
    override val factoryContext: DefaultRepositoryFactoryContext
) : DefaultRepositoryImpl(reactionRemoteDataSource, factoryContext.collectionPath), ReactionRepository {

    /**
     * 특정 메시지에 대한 반응 목록을 스트림으로 조회합니다.
     * 
     * @param messageId 반응을 조회할 메시지 ID
     * @return 반응 목록 스트림
     */
    override fun getReactionsStream(messageId: String): Flow<CustomResult<List<Reaction>, Exception>> {
        // 데이터 소스가 구현되기 전까지는 빈 리스트 반환
        return flowOf(CustomResult.Failure(Exception("ReactionRemoteDataSource가 아직 구현되지 않았습니다.")))
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Reaction)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Reaction"))

        return if (entity.id.isAssigned()) {
            reactionRemoteDataSource.update(entity.id, entity.getChangedFields())
        } else {
            reactionRemoteDataSource.create(entity.toDto())
        }
    }

}
