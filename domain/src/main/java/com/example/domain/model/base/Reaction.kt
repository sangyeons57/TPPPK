package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.domain.model.AggregateRoot
import com.example.domain.event.reaction.ReactionAddedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.reaction.Emoji
import com.example.core_common.util.DateTimeUtil

class Reaction private constructor(
    initialUserId: UserId, // The user who reacted
    initialEmoji: Emoji,      // The unicode emoji character
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    init {
        setOriginalState()
    }

    val userId: UserId = initialUserId
    val emoji: Emoji = initialEmoji

    /**
     * A Reaction's state is immutable once created.
     * There are no properties to update, so this map is empty.
     */
    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_USER_ID to this.userId.value,
            KEY_EMOJI to this.emoji.value,
            KEY_CREATED_AT to this.createdAt,
            KEY_UPDATED_AT to this.updatedAt,
        )
    }

    companion object {
        const val COLLECTION_NAME = "reactions"
        const val KEY_USER_ID = "userId"
        const val KEY_EMOJI = "emoji"
        /**
         * Factory method for adding a new reaction.
         */
        fun create(id: DocumentId, userId: UserId, emoji: Emoji, messageId: DocumentId): Reaction {
            val reaction = Reaction(
                initialUserId = userId,
                initialEmoji = emoji,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                id = id,
                isNew = true
            )
            return reaction
        }

        /**
         * Factory method to reconstitute a Reaction from a data source.
         */
        fun fromDataSource(
            id: DocumentId,
            userId: UserId,
            emoji: Emoji,
            createdAt: Instant?,
            updatedAt: Instant?
        ): Reaction {
            return Reaction(
                initialUserId = userId,
                initialEmoji = emoji,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                id = id,
                isNew = false
            )
        }
    }
}
