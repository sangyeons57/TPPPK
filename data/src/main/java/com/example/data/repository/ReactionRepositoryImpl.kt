package com.example.data.repository

import com.example.core_common.result.CustomResult
// import com.example.data.datasource.remote.ReactionRemoteDataSource // TODO: DataSource 생성 후 주석 해제
import com.example.domain.model.base.Reaction
import com.example.domain.repository.ReactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import javax.inject.Inject

/**
 * 메시지 반응(이모지) 관리를 위한 저장소 구현체
 * 현재는 임시 구현으로, ReactionRemoteDataSource가 구현되면 실제 구현으로 교체 예정
 */
class ReactionRepositoryImpl @Inject constructor(
    // private val reactionRemoteDataSource: ReactionRemoteDataSource // TODO: DataSource 생성 후 주석 해제
) : ReactionRepository {

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

    /**
     * 메시지에 새 반응을 추가합니다.
     * 
     * @param messageId 반응을 추가할 메시지 ID
     * @param userId 반응을 추가하는 사용자 ID
     * @param emoji 추가할 이모지 문자
     * @return 추가된 반응 정보
     */
    override suspend fun addReaction(messageId: String, userId: String, emoji: String): CustomResult<Reaction, Exception> {
        // 데이터 소스가 구현되기 전까지는 실패 결과 반환
        return CustomResult.Failure(Exception("ReactionRemoteDataSource가 아직 구현되지 않았습니다."))
    }

    /**
     * 기존 반응을 제거합니다.
     * 
     * @param reactionId 제거할 반응 ID
     * @return 성공 시 Result.success(Unit), 실패 시 Result.failure
     */
    override suspend fun removeReaction(reactionId: String): CustomResult<Unit, Exception> {
        // 데이터 소스가 구현되기 전까지는 실패 결과 반환
        return CustomResult.Failure(Exception("ReactionRemoteDataSource가 아직 구현되지 않았습니다."))
    }
}
