package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.LocalReactionsDataSource
import com.example.domain.model.base.Reaction
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.reaction.Emoji
import com.example.domain.repository.local.LocalReactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Reaction Repository Implementation (SSOT)
 * Room Database 전용 구현체 - UI에 직접 데이터 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - LocalDataSource를 통한 Room DB 접근
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 */
@Singleton
class LocalReactionRepositoryImpl @Inject constructor(
    private val localReactionsDataSource: LocalReactionsDataSource
) : LocalReactionRepository {

    companion object {
        private const val TAG = "LocalReactionRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeReactionById(reactionId: String): Flow<Reaction?> {
        Log.d(TAG, "observeReactionById: $reactionId")
        return localReactionsDataSource.observeReactionById(reactionId)
    }

    override fun observeReactionsByMessage(messageId: String): Flow<List<Reaction>> {
        Log.d(TAG, "observeReactionsByMessage: $messageId")
        return localReactionsDataSource.observeReactionsByMessage(messageId)
    }

    override fun observeReactionsByUser(userId: String): Flow<List<Reaction>> {
        Log.d(TAG, "observeReactionsByUser: $userId")
        return localReactionsDataSource.observeReactionsByUser(userId)
    }

    override fun observeReactionsByEmoji(emoji: Emoji): Flow<List<Reaction>> {
        Log.d(TAG, "observeReactionsByEmoji: $emoji")
        return localReactionsDataSource.observeReactionsByEmoji(emoji.value)
    }

    override fun observeReactionsByMessageAndUser(
        messageId: String,
        userId: String
    ): Flow<List<Reaction>> {
        Log.d(TAG, "observeReactionsByMessageAndUser: $messageId, $userId")
        return localReactionsDataSource.observeReactionsByMessageAndUser(messageId, userId)
    }

    override fun observeReactionsByMessageAndEmoji(
        messageId: String,
        emoji: Emoji
    ): Flow<List<Reaction>> {
        Log.d(TAG, "observeReactionsByMessageAndEmoji: $messageId, $emoji")
        return localReactionsDataSource.observeReactionsByMessageAndEmoji(messageId, emoji.value)
    }

    override fun observeReactionsByUserAndEmoji(
        userId: String,
        emoji: Emoji
    ): Flow<List<Reaction>> {
        Log.d(TAG, "observeReactionsByUserAndEmoji: $userId, $emoji")
        return localReactionsDataSource.observeReactionsByUserAndEmoji(userId, emoji.value)
    }

    override fun observeAllReactions(): Flow<List<Reaction>> {
        Log.d(TAG, "observeAllReactions")
        return localReactionsDataSource.observeAllReactions()
    }

    override fun observeReactionUpdatedAt(reactionId: String): Flow<Long?> {
        Log.d(TAG, "observeReactionUpdatedAt: $reactionId")
        return localReactionsDataSource.observeReactionUpdatedAt(reactionId)
    }

    override fun observeReactions(reactionIds: List<String>): Flow<List<Reaction>> {
        Log.d(TAG, "observeReactions: ${reactionIds.size} reactions")
        return localReactionsDataSource.observeReactions(reactionIds)
    }

    override fun observeReactionCountsByMessage(messageId: String): Flow<Map<Emoji, Int>> {
        Log.d(TAG, "observeReactionCountsByMessage: $messageId")
        return localReactionsDataSource.observeReactionStatsByMessage(messageId).map { map ->
            map.mapKeys { Emoji(it.key) }
        }
    }

    override fun observeRecentReactions(timestamp: Instant): Flow<List<Reaction>> {
        Log.d(TAG, "observeRecentReactions: $timestamp")
        return localReactionsDataSource.observeRecentReactions(timestamp)
    }

    // === 단순 읽기 작업 ===

    override suspend fun getReactionById(reactionId: String): Reaction? {
        Log.d(TAG, "getReactionById: $reactionId")
        return try {
            localReactionsDataSource.getReactionById(reactionId)
        } catch (e: Exception) {
            Log.e(TAG, "getReactionById failed", e)
            null
        }
    }

    override suspend fun getReactionsByMessage(messageId: String): List<Reaction> {
        Log.d(TAG, "getReactionsByMessage: $messageId")
        return try {
            localReactionsDataSource.getReactionsByMessage(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "getReactionsByMessage failed", e)
            emptyList()
        }
    }

    override suspend fun getReactionsByUser(userId: String): List<Reaction> {
        Log.d(TAG, "getReactionsByUser: $userId")
        return try {
            localReactionsDataSource.getReactionsByUser(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getReactionsByUser failed", e)
            emptyList()
        }
    }

    override suspend fun getReactionsByEmoji(emoji: Emoji): List<Reaction> {
        Log.d(TAG, "getReactionsByEmoji: $emoji")
        return try {
            localReactionsDataSource.getReactionsByEmoji(emoji.value)
        } catch (e: Exception) {
            Log.e(TAG, "getReactionsByEmoji failed", e)
            emptyList()
        }
    }

    override suspend fun getReactionsByMessageAndUser(
        messageId: String,
        userId: String
    ): List<Reaction> {
        Log.d(TAG, "getReactionsByMessageAndUser: $messageId, $userId")
        return try {
            localReactionsDataSource.getReactionsByMessageAndUser(messageId, userId)
        } catch (e: Exception) {
            Log.e(TAG, "getReactionsByMessageAndUser failed", e)
            emptyList()
        }
    }

    override suspend fun getReactionsByMessageAndEmoji(
        messageId: String,
        emoji: Emoji
    ): List<Reaction> {
        Log.d(TAG, "getReactionsByMessageAndEmoji: $messageId, $emoji")
        return try {
            localReactionsDataSource.getReactionsByMessageAndEmoji(messageId, emoji.value)
        } catch (e: Exception) {
            Log.e(TAG, "getReactionsByMessageAndEmoji failed", e)
            emptyList()
        }
    }

    override suspend fun getReactionsByUserAndEmoji(userId: String, emoji: Emoji): List<Reaction> {
        Log.d(TAG, "getReactionsByUserAndEmoji: $userId, $emoji")
        return try {
            localReactionsDataSource.getReactionsByUserAndEmoji(userId, emoji.value)
        } catch (e: Exception) {
            Log.e(TAG, "getReactionsByUserAndEmoji failed", e)
            emptyList()
        }
    }

    override suspend fun getAllReactions(limit: Int?): List<Reaction> {
        Log.d(TAG, "getAllReactions: limit=$limit")
        return try {
            localReactionsDataSource.getAllReactions(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getAllReactions failed", e)
            emptyList()
        }
    }

    override suspend fun getReactionsByIds(reactionIds: List<String>): List<Reaction> {
        Log.d(TAG, "getReactionsByIds: ${reactionIds.size} reactions")
        return try {
            localReactionsDataSource.getReactionsByIds(reactionIds)
        } catch (e: Exception) {
            Log.e(TAG, "getReactionsByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getReactionCountsByMessage(messageId: String): Map<Emoji, Int> {
        Log.d(TAG, "getReactionCountsByMessage: $messageId")
        return try {
            localReactionsDataSource.getReactionStatsByMessage(messageId).mapKeys { Emoji(it.key) }
        } catch (e: Exception) {
            Log.e(TAG, "getReactionCountsByMessage failed", e)
            emptyMap()
        }
    }

    override suspend fun getUniqueEmojisByMessage(messageId: String): List<Emoji> {
        Log.d(TAG, "getUniqueEmojisByMessage: $messageId")
        return try {
            localReactionsDataSource.getReactionStatsByMessage(messageId).keys.map { Emoji(it) }.toList()
        } catch (e: Exception) {
            Log.e(TAG, "getUniqueEmojisByMessage failed", e)
            emptyList()
        }
    }

    override suspend fun getRecentReactions(timestamp: Instant): List<Reaction> {
        Log.d(TAG, "getRecentReactions: $timestamp")
        return try {
            localReactionsDataSource.getRecentReactions(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getRecentReactions failed", e)
            emptyList()
        }
    }

    override suspend fun getReactionCount(messageId: String, emoji: Emoji): Int {
        Log.d(TAG, "getReactionCount: $messageId, $emoji")
        return try {
            localReactionsDataSource.getReactionCountByMessageAndEmoji(messageId, emoji.value)
        } catch (e: Exception) {
            Log.e(TAG, "getReactionCount failed", e)
            0
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveReaction(reaction: Reaction): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveReaction: ${reaction.id}")

            // 1. Room DB에 저장
            localReactionsDataSource.saveReaction(reaction)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (reaction.isNew) "CREATE" else "UPDATE"
            localReactionsDataSource.addToOutbox(
                reactionId = reaction.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Reaction saved and added to outbox: ${reaction.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveReaction failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveReactions(reactions: List<Reaction>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveReactions: ${reactions.size} reactions")

            if (reactions.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localReactionsDataSource.saveReactions(reactions)

            Log.d(TAG, "Bulk reactions saved: ${reactions.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveReactions failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteReaction(reactionId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteReaction: $reactionId")

            // 1. Room DB에서 삭제 (Hard delete for reactions)
            localReactionsDataSource.deleteReaction(reactionId)

            // 2. Outbox에 삭제 작업 추가
            localReactionsDataSource.addToOutbox(
                reactionId = reactionId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Reaction deleted and added to outbox: $reactionId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteReaction failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteReactionsByMessage(messageId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteReactionsByMessage: $messageId")

            localReactionsDataSource.deleteReactionsByMessage(messageId)

            Log.d(TAG, "Reactions deleted for message: $messageId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteReactionsByMessage failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteReactionsByUser(userId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteReactionsByUser: $userId")

            localReactionsDataSource.deleteReactionsByUser(userId)

            Log.d(TAG, "Reactions deleted for user: $userId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteReactionsByUser failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteReactionsByMessageAndUser(
        messageId: String,
        userId: String
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteReactionsByMessageAndUser: $messageId, $userId")

            localReactionsDataSource.deleteReactionByMessageAndUser(messageId, userId)

            Log.d(TAG, "Reactions deleted for message $messageId by user $userId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteReactionsByMessageAndUser failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteReactionsByMessageAndEmoji(
        messageId: String,
        emoji: Emoji
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteReactionsByMessageAndEmoji: $messageId, $emoji")

            localReactionsDataSource.deleteReactionsByMessageAndEmoji(messageId, emoji.value)

            Log.d(TAG, "Reactions deleted for message $messageId with emoji $emoji")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteReactionsByMessageAndEmoji failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun toggleReaction(
        messageId: String,
        userId: String,
        emoji: Emoji
    ): CustomResult<Boolean, Exception> {
        return try {
            Log.d(TAG, "toggleReaction: messageId=$messageId, userId=$userId, emoji=$emoji")

            // 1. 기존 반응 확인
            val existingReactions = localReactionsDataSource.getReactionsByMessageAndUser(messageId, userId)
                .filter { it.emoji == emoji }

            if (existingReactions.isNotEmpty()) {
                // 반응이 있으면 삭제
                existingReactions.forEach { reaction ->
                    deleteReaction(reaction.id.value)
                }
                Log.d(TAG, "Reaction removed: $messageId, $userId, $emoji")
                CustomResult.Success(false) // false = 삭제됨
            } else {
                // 반응이 없으면 추가
                val newReaction = Reaction.create(
                    id = DocumentId.generate(),
                    userId = UserId(userId),
                    emoji = emoji,
                    messageId = DocumentId(messageId)
                )
                saveReaction(newReaction)
                Log.d(TAG, "Reaction added: $messageId, $userId, $emoji")
                CustomResult.Success(true) // true = 추가됨
            }

        } catch (e: Exception) {
            Log.e(TAG, "toggleReaction failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 유틸리티 ===

    override suspend fun reactionExists(reactionId: String): Boolean {
        return try {
            localReactionsDataSource.reactionExists(reactionId)
        } catch (e: Exception) {
            Log.e(TAG, "reactionExists failed", e)
            false
        }
    }

    override suspend fun reactionExists(messageId: String, userId: String, emoji: Emoji): Boolean {
        return try {
            localReactionsDataSource.hasUserReactedWithEmoji(messageId, userId, emoji.value)
        } catch (e: Exception) {
            Log.e(TAG, "reactionExists failed", e)
            false
        }
    }

    override suspend fun getReactionCountByMessage(messageId: String): Int {
        return try {
            localReactionsDataSource.getReactionCount(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "getReactionCountByMessage failed", e)
            0
        }
    }

    override suspend fun getReactionCountByUser(userId: String): Int {
        return try {
            localReactionsDataSource.getReactionCountByUser(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getReactionCountByUser failed", e)
            0
        }
    }

    override suspend fun getReactionCountByEmoji(emoji: Emoji): Int {
        return try {
            localReactionsDataSource.getReactionCountByEmoji(emoji.value)
        } catch (e: Exception) {
            Log.e(TAG, "getReactionCountByEmoji failed", e)
            0
        }
    }

    override suspend fun getTotalReactionCount(): Int {
        return try {
            localReactionsDataSource.getTotalReactionCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalReactionCount failed", e)
            0
        }
    }

    override suspend fun getUniqueEmojiCountByMessage(messageId: String): Int {
        return try {
            localReactionsDataSource.getReactionStatsByMessage(messageId).keys.size
        } catch (e: Exception) {
            Log.e(TAG, "getUniqueEmojiCountByMessage failed", e)
            0
        }
    }

    override suspend fun clearAllReactions(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllReactions")

            localReactionsDataSource.clearAllReactions()

            Log.d(TAG, "All reactions cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllReactions failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getReactionsUpdatedAfter(timestamp: Instant): List<Reaction> {
        return try {
            localReactionsDataSource.getReactionsUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getReactionsUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        reactionId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: reactionId=$reactionId, operation=$operation")

            localReactionsDataSource.addToOutbox(reactionId, operation, payload)

            Log.d(TAG, "Added to outbox: $reactionId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}