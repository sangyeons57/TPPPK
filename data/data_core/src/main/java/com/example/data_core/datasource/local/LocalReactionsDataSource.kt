package com.example.data_core.datasource.local

import com.example.domain.model.base.Reaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 로컬 리액션 데이터 저장소 인터페이스
 * 3-tier 클라이언트 주도 동기화 아키텍처를 지원합니다
 */
interface LocalReactionsDataSource {

    // === 기본 CRUD 작업 ===

    /**
     * 리액션 ID로 단일 리액션 조회
     * @param reactionId 리액션 ID
     * @return 리액션 정보 (없으면 null)
     */
    suspend fun getReactionById(reactionId: String): Reaction?

    /**
     * 메시지별 리액션 목록 조회
     * @param messageId 메시지 ID
     * @return 리액션 목록
     */
    suspend fun getReactionsByMessage(messageId: String): List<Reaction>

    /**
     * 메시지와 이모지별 리액션 조회
     * @param messageId 메시지 ID
     * @param emoji 이모지
     * @return 리액션 목록
     */
    suspend fun getReactionsByMessageAndEmoji(messageId: String, emoji: String): List<Reaction>

    /**
     * 사용자별 리액션 목록 조회
     * @param userId 사용자 ID
     * @return 리액션 목록
     */
    suspend fun getReactionsByUser(userId: String): List<Reaction>

    /**
     * 사용자가 특정 메시지에 추가한 리액션 조회
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 리액션 목록
     */
    suspend fun getReactionsByMessageAndUser(messageId: String, userId: String): List<Reaction>

    /**
     * 특정 이모지의 모든 리액션 조회
     * @param emoji 이모지
     * @return 리액션 목록
     */
    suspend fun getReactionsByEmoji(emoji: String): List<Reaction>

    /**
     * 채널별 리액션 목록 조회
     * @param channelId 채널 ID
     * @return 리액션 목록
     */
    suspend fun getReactionsByChannel(channelId: String): List<Reaction>

    /**
     * 단일 리액션 저장
     * @param reaction 저장할 리액션
     */
    suspend fun saveReaction(reaction: Reaction)

    /**
     * 리액션 목록 배치 저장
     * @param reactions 저장할 리액션 목록
     */
    suspend fun saveReactions(reactions: List<Reaction>)

    /**
     * 리액션 삭제
     * @param reactionId 삭제할 리액션 ID
     */
    suspend fun deleteReaction(reactionId: String)

    /**
     * 메시지별 리액션 일괄 삭제
     * @param messageId 메시지 ID
     */
    suspend fun deleteReactionsByMessage(messageId: String)

    /**
     * 사용자별 리액션 일괄 삭제
     * @param userId 사용자 ID
     */
    suspend fun deleteReactionsByUser(userId: String)

    /**
     * 특정 메시지의 특정 사용자 리액션 삭제
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emoji 이모지 (선택적)
     */
    suspend fun deleteReactionByMessageAndUser(
        messageId: String,
        userId: String,
        emoji: String? = null
    )

    // === 3-tier 동기화 지원 ===

    /**
     * 특정 시점 이후 업데이트된 리액션들을 조회 (증분 동기화용)
     * @param timestamp 기준 시간
     * @return 업데이트된 리액션 목록
     */
    suspend fun getReactionsUpdatedAfter(timestamp: Instant): List<Reaction>

    /**
     * 메시지별 리액션 실시간 관찰
     * @param messageId 메시지 ID
     * @return 리액션 목록 Flow
     */
    fun observeReactionsByMessage(messageId: String): Flow<List<Reaction>>

    /**
     * 특정 리액션 실시간 관찰
     * @param reactionId 리액션 ID
     * @return 리액션 정보 Flow
     */
    fun observeReactionById(reactionId: String): Flow<Reaction?>

    /**
     * 메시지와 이모지별 리액션 수 실시간 관찰
     * @param messageId 메시지 ID
     * @param emoji 이모지
     * @return 리액션 수 Flow
     */
    fun observeReactionCountByMessageAndEmoji(messageId: String, emoji: String): Flow<Int>

    /**
     * 메시지별 리액션 통계 실시간 관찰
     * @param messageId 메시지 ID
     * @return 이모지별 리액션 수 Map Flow
     */
    fun observeReactionStatsByMessage(messageId: String): Flow<Map<String, Int>>

    // === Outbox 관리 ===

    /**
     * 리액션 변경사항을 Outbox에 기록
     * @param reactionId 리액션 ID
     * @param operation 작업 유형 (CREATE, UPDATE, DELETE)
     * @param payload 변경 데이터 (선택적)
     */
    suspend fun addToOutbox(reactionId: String, operation: String, payload: String? = null)

    /**
     * 대기 중인 Outbox 작업 목록 조회
     * @return 대기 중인 작업 목록
     */
    suspend fun getPendingOutboxOperations(): List<ReactionOutboxOperation>

    /**
     * Outbox 작업 완료 처리
     * @param operationId 작업 ID
     */
    suspend fun markOutboxOperationComplete(operationId: String)

    /**
     * Outbox 작업 재시도 증가
     * @param operationId 작업 ID
     */
    suspend fun incrementOutboxRetries(operationId: String)

    // === 동기화 메타데이터 관리 ===

    /**
     * 마지막 동기화 커서 조회
     * @return 마지막 서버 커서 (밀리초)
     */
    suspend fun getLastSyncCursor(): Long?

    /**
     * 동기화 커서 업데이트
     * @param cursor 새로운 서버 커서
     * @param timestamp 동기화 시간
     */
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long)

    // === 유틸리티 ===

    /**
     * 리액션 존재 여부 확인
     * @param reactionId 리액션 ID
     * @return 존재 여부
     */
    suspend fun reactionExists(reactionId: String): Boolean

    /**
     * 사용자가 특정 메시지에 특정 이모지로 리액션했는지 확인
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emoji 이모지
     * @return 리액션 존재 여부
     */
    suspend fun hasUserReactedWithEmoji(messageId: String, userId: String, emoji: String): Boolean

    /**
     * 메시지별 리액션 수 조회
     * @param messageId 메시지 ID
     * @return 리액션 수
     */
    suspend fun getReactionCount(messageId: String): Int

    /**
     * 전체 리액션 수 조회
     * @return 전체 리액션 수
     */
    suspend fun getTotalReactionCount(): Int

    /**
     * 메시지별 이모지 통계 조회
     * @param messageId 메시지 ID
     * @return 이모지별 리액션 수 Map
     */
    suspend fun getReactionStatsByMessage(messageId: String): Map<String, Int>

    /**
     * 인기 이모지 목록 조회
     * @param limit 조회할 이모지 수
     * @return 이모지와 사용 횟수 Map
     */
    suspend fun getPopularEmojis(limit: Int = 10): Map<String, Int>

    /**
     * 모든 리액션 데이터 삭제 (개발/테스트용)
     */
    suspend fun clearAllReactions()
}

/**
 * 리액션 Outbox 작업 정보
 */
data class ReactionOutboxOperation(
    val id: String,
    val reactionId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)