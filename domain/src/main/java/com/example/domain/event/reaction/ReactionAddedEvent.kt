package com.example.domain.event.reaction

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import java.time.Instant

/**
 * Event indicating that a new reaction has been added to a message.
 */
data class ReactionAddedEvent(
    val reactionId: DocumentId,
    val messageId: DocumentId, // The message this reaction is for
    val userId: UserId,
    val emoji: String,
    override val occurredOn: Instant
) : DomainEvent
