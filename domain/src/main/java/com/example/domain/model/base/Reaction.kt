package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.reaction.Emoji
import java.time.Instant

class Reaction private constructor(
    initialUserId: UserId, // The user who reacted
    initialEmoji: Emoji,      // The unicode emoji character
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    val userId: UserId = initialUserId
    val emoji: Emoji = initialEmoji



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
