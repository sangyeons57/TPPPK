package com.example.domain.event.message

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.message.MessageContent
import java.time.Instant

/**
 * Event indicating that a message's content has been updated.
 */
data class MessageContentUpdatedEvent(
    val messageId: DocumentId,
    val newContent: MessageContent,
    override val occurredOn: Instant
) : DomainEvent
