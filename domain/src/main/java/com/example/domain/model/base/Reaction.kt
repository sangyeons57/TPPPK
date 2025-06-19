package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import com.example.domain.event.AggregateRoot
import com.example.domain.event.reaction.ReactionAddedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId

class Reaction private constructor(
    initialUserId: UserId, // The user who reacted
    initialEmoji: String,      // The unicode emoji character
    initialCreatedAt: Instant,
    override val id: DocumentId,
    override val isNew: Boolean,
) : AggregateRoot() {

    val userId: UserId = initialUserId
    val emoji: String = initialEmoji
    val createdAt: Instant = initialCreatedAt

    /**
     * A Reaction's state is immutable once created.
     * There are no properties to update, so this map is empty.
     */
    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_USER_ID to this.userId,
            KEY_EMOJI to this.emoji,
            KEY_CREATED_AT to this.createdAt,
        )
    }

    companion object {
        const val COLLECTION_NAME = "reactions"
        const val KEY_USER_ID = "userId"
        const val KEY_EMOJI = "emoji"
        const val KEY_CREATED_AT = "createdAt"
        /**
         * Factory method for adding a new reaction.
         */
        fun create(id: DocumentId, userId: UserId, emoji: String, messageId: DocumentId): Reaction {
            val now = Instant.now()
            val reaction = Reaction(
                initialUserId = userId,
                initialEmoji = emoji,
                initialCreatedAt = now,
                id = id,
                isNew = true
            )
            reaction.pushDomainEvent(
                ReactionAddedEvent(
                    reactionId = reaction.id,
                    messageId = messageId,
                    userId = reaction.userId,
                    emoji = reaction.emoji,
                    occurredOn = now
                )
            )
            return reaction
        }

        /**
         * Factory method to reconstitute a Reaction from a data source.
         */
        fun fromDataSource(
            id: DocumentId,
            userId: UserId,
            emoji: String,
            createdAt: Instant
        ): Reaction {
            return Reaction(
                initialUserId = userId,
                initialEmoji = emoji,
                initialCreatedAt = createdAt,
                id = id,
                isNew = false
            )
        }
    }
}
