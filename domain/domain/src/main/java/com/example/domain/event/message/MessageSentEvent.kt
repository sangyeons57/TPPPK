package com.example.domain.event.message

import com.example.domain.event.DomainEvent
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MessagePayload
import java.time.Instant

/**
 * Event indicating that a new message has been sent.
 */
data class MessageSentEvent(
    val messageId: DocumentId,
    val senderId: UserId,
    val content: MessagePayload,
    val replyToMessageId: DocumentId?,
    override val occurredOn: Instant
) : DomainEvent
