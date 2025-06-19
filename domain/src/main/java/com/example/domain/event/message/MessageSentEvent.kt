package com.example.domain.event.message

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import java.time.Instant

/**
 * Event indicating that a new message has been sent.
 */
data class MessageSentEvent(
    val messageId: DocumentId,
    val senderId: UserId,
    val content: MessageContent,
    val replyToMessageId: DocumentId?,
    override val occurredOn: Instant
) : DomainEvent
