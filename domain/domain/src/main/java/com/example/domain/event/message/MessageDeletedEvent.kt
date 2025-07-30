package com.example.domain.event.message

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a message has been marked as deleted.
 */
data class MessageDeletedEvent(
    val messageId: DocumentId,
    override val occurredOn: Instant
) : DomainEvent
