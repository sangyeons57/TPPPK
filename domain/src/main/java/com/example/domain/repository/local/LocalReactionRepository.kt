package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Reaction
import com.example.domain.model.vo.reaction.Emoji
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Reaction Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (Remote ReactionRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - Reaction 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observeReactionById
 * - observeAllEntities -> observeAllReactions
 * - observeEntityUpdatedAt -> observeReactionUpdatedAt
 * - getEntityById -> getReactionById
 * - getEntitiesByIds -> getReactionsByIds
 * - getAllEntities -> getAllReactions
 * - saveEntity -> saveReaction
 * - saveEntities -> saveReactions
 * - deleteEntity -> deleteReaction
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalReactionRepository : BaseLocalRepository<Reaction> {

    // === BaseLocalRepository 메서드 (구현체에서 반응 전용 메서드로 매핑) ===
    // observeEntityById -> observeReactionById
    // observeAllEntities -> observeAllReactions  
    // observeEntityUpdatedAt -> observeReactionUpdatedAt
    // getEntityById -> getReactionById
    // getEntitiesByIds -> getReactionsByIds
    // getAllEntities -> getAllReactions
    // saveEntity -> saveReaction
    // saveEntities -> saveReactions
    // deleteEntity -> deleteReaction
    // getEntitiesUpdatedAfter -> getReactionsUpdatedAfter
    // clearAllEntities -> clearAllReactions
    // getTotalEntityCount -> getTotalReactionCount
    // entityExists -> reactionExists

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 반응을 실시간 관찰
     * @param reactionId 반응 ID
     * @return 반응 Flow (null 가능)
     */
    fun observeReactionById(reactionId: String): Flow<Reaction?>

    /**
     * 메시지의 모든 반응을 실시간 관찰
     * @param messageId 메시지 ID
     * @return 반응 목록 Flow
     */
    fun observeReactionsByMessage(messageId: String): Flow<List<Reaction>>

    /**
     * 사용자의 모든 반응을 실시간 관찰
     * @param userId 사용자 ID
     * @return 반응 목록 Flow
     */
    fun observeReactionsByUser(userId: String): Flow<List<Reaction>>

    /**
     * 특정 이모지의 모든 반응을 실시간 관찰
     * @param emoji 이모지
     * @return 반응 목록 Flow
     */
    fun observeReactionsByEmoji(emoji: Emoji): Flow<List<Reaction>>

    /**
     * 메시지와 사용자로 특정 반응을 실시간 관찰
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 반응 목록 Flow
     */
    fun observeReactionsByMessageAndUser(messageId: String, userId: String): Flow<List<Reaction>>

    /**
     * 메시지와 이모지로 특정 반응을 실시간 관찰
     * @param messageId 메시지 ID
     * @param emoji 이모지
     * @return 반응 목록 Flow
     */
    fun observeReactionsByMessageAndEmoji(messageId: String, emoji: Emoji): Flow<List<Reaction>>

    /**
     * 사용자와 이모지로 특정 반응을 실시간 관찰
     * @param userId 사용자 ID
     * @param emoji 이모지
     * @return 반응 목록 Flow
     */
    fun observeReactionsByUserAndEmoji(userId: String, emoji: Emoji): Flow<List<Reaction>>

    /**
     * 모든 반응을 실시간 관찰
     * @return 전체 반응 목록 Flow
     */
    fun observeAllReactions(): Flow<List<Reaction>>

    /**
     * 특정 반응의 updatedAt 필드 변경을 실시간 관찰
     * @param reactionId 반응 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeReactionUpdatedAt(reactionId: String): Flow<Long?>

    /**
     * 주어진 ID 목록에 해당하는 반응 목록을 실시간 관찰
     * @param reactionIds 반응 ID 목록
     * @return 반응 목록 Flow
     */
    fun observeReactions(reactionIds: List<String>): Flow<List<Reaction>>

    /**
     * 메시지의 반응을 이모지별로 그룹화하여 실시간 관찰
     * @param messageId 메시지 ID
     * @return 이모지별 반응 수 맵 Flow
     */
    fun observeReactionCountsByMessage(messageId: String): Flow<Map<Emoji, Int>>

    /**
     * 특정 시간 이후 생성된 반응들을 실시간 관찰
     * @param timestamp 기준 시간
     * @return 최근 반응 목록 Flow
     */
    fun observeRecentReactions(timestamp: Instant): Flow<List<Reaction>>

    // === 단순 읽기 작업 ===

    /**
     * 반응 ID로 조회
     * @param reactionId 반응 ID
     * @return 반응 (없으면 null)
     */
    suspend fun getReactionById(reactionId: String): Reaction?

    /**
     * 메시지의 모든 반응 조회
     * @param messageId 메시지 ID
     * @return 반응 목록
     */
    suspend fun getReactionsByMessage(messageId: String): List<Reaction>

    /**
     * 사용자의 모든 반응 조회
     * @param userId 사용자 ID
     * @return 반응 목록
     */
    suspend fun getReactionsByUser(userId: String): List<Reaction>

    /**
     * 특정 이모지의 모든 반응 조회
     * @param emoji 이모지
     * @return 반응 목록
     */
    suspend fun getReactionsByEmoji(emoji: Emoji): List<Reaction>

    /**
     * 메시지와 사용자로 특정 반응 조회
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 반응 목록
     */
    suspend fun getReactionsByMessageAndUser(messageId: String, userId: String): List<Reaction>

    /**
     * 메시지와 이모지로 특정 반응 조회
     * @param messageId 메시지 ID
     * @param emoji 이모지
     * @return 반응 목록
     */
    suspend fun getReactionsByMessageAndEmoji(messageId: String, emoji: Emoji): List<Reaction>

    /**
     * 사용자와 이모지로 특정 반응 조회
     * @param userId 사용자 ID
     * @param emoji 이모지
     * @return 반응 목록
     */
    suspend fun getReactionsByUserAndEmoji(userId: String, emoji: Emoji): List<Reaction>

    /**
     * 모든 반응 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 반응 목록
     */
    suspend fun getAllReactions(limit: Int? = null): List<Reaction>

    /**
     * 여러 반응 ID로 조회
     * @param reactionIds 반응 ID 목록
     * @return 반응 목록
     */
    suspend fun getReactionsByIds(reactionIds: List<String>): List<Reaction>

    /**
     * 메시지의 반응을 이모지별로 그룹화하여 조회
     * @param messageId 메시지 ID
     * @return 이모지별 반응 수 맵
     */
    suspend fun getReactionCountsByMessage(messageId: String): Map<Emoji, Int>

    /**
     * 메시지의 고유 이모지 목록 조회
     * @param messageId 메시지 ID
     * @return 고유 이모지 목록
     */
    suspend fun getUniqueEmojisByMessage(messageId: String): List<Emoji>

    /**
     * 특정 시간 이후 생성된 반응들 조회
     * @param timestamp 기준 시간
     * @return 최근 반응 목록
     */
    suspend fun getRecentReactions(timestamp: Instant): List<Reaction>

    /**
     * 메시지의 특정 이모지 반응 수 조회
     * @param messageId 메시지 ID
     * @param emoji 이모지
     * @return 반응 수
     */
    suspend fun getReactionCount(messageId: String, emoji: Emoji): Int

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 반응 저장 (생성/수정)
     * @param reaction 저장할 반응
     * @return 성공 여부
     */
    suspend fun saveReaction(reaction: Reaction): CustomResult<Unit, Exception>

    /**
     * 반응 대량 저장 (동기화용)
     * @param reactions 저장할 반응 목록
     * @return 성공 여부
     */
    suspend fun saveReactions(reactions: List<Reaction>): CustomResult<Unit, Exception>

    /**
     * 반응 삭제 (Hard Delete - 반응은 수정되지 않고 추가/삭제만 됨)
     * @param reactionId 반응 ID
     * @return 성공 여부
     */
    suspend fun deleteReaction(reactionId: String): CustomResult<Unit, Exception>

    /**
     * 메시지의 모든 반응 삭제
     * @param messageId 메시지 ID
     * @return 성공 여부
     */
    suspend fun deleteReactionsByMessage(messageId: String): CustomResult<Unit, Exception>

    /**
     * 사용자의 모든 반응 삭제
     * @param userId 사용자 ID
     * @return 성공 여부
     */
    suspend fun deleteReactionsByUser(userId: String): CustomResult<Unit, Exception>

    /**
     * 특정 메시지의 특정 사용자 반응 삭제
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 성공 여부
     */
    suspend fun deleteReactionsByMessageAndUser(
        messageId: String,
        userId: String
    ): CustomResult<Unit, Exception>

    /**
     * 특정 메시지의 특정 이모지 반응 삭제
     * @param messageId 메시지 ID
     * @param emoji 이모지
     * @return 성공 여부
     */
    suspend fun deleteReactionsByMessageAndEmoji(
        messageId: String,
        emoji: Emoji
    ): CustomResult<Unit, Exception>

    /**
     * 반응 토글 (있으면 삭제, 없으면 추가)
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emoji 이모지
     * @return 성공 여부와 토글 결과 (true: 추가됨, false: 삭제됨)
     */
    suspend fun toggleReaction(
        messageId: String,
        userId: String,
        emoji: Emoji
    ): CustomResult<Boolean, Exception>

    // === 유틸리티 ===

    /**
     * 반응 존재 여부 확인
     * @param reactionId 반응 ID
     * @return 존재 여부
     */
    suspend fun reactionExists(reactionId: String): Boolean

    /**
     * 특정 조건의 반응 존재 여부 확인
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emoji 이모지
     * @return 존재 여부
     */
    suspend fun reactionExists(messageId: String, userId: String, emoji: Emoji): Boolean

    /**
     * 메시지별 반응 수 조회
     * @param messageId 메시지 ID
     * @return 반응 수
     */
    suspend fun getReactionCountByMessage(messageId: String): Int

    /**
     * 사용자별 반응 수 조회
     * @param userId 사용자 ID
     * @return 반응 수
     */
    suspend fun getReactionCountByUser(userId: String): Int

    /**
     * 이모지별 반응 수 조회
     * @param emoji 이모지
     * @return 반응 수
     */
    suspend fun getReactionCountByEmoji(emoji: Emoji): Int

    /**
     * 전체 반응 수 조회
     * @return 반응 수
     */
    suspend fun getTotalReactionCount(): Int

    /**
     * 메시지의 고유 이모지 수 조회
     * @param messageId 메시지 ID
     * @return 고유 이모지 수
     */
    suspend fun getUniqueEmojiCountByMessage(messageId: String): Int

    /**
     * 모든 반응 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllReactions(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 반응 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 반응 목록
     */
    suspend fun getReactionsUpdatedAfter(timestamp: Instant): List<Reaction>

    /**
     * Outbox에 작업 추가 (서버 동기화 대기열)
     * @param reactionId 반응 ID
     * @param operation 작업 타입 (CREATE, DELETE)
     * @param payload 작업 데이터 (JSON)
     * @return 성공 여부
     */
    suspend fun addToOutbox(
        reactionId: String,
        operation: String,
        payload: String? = null
    ): CustomResult<Unit, Exception>
}