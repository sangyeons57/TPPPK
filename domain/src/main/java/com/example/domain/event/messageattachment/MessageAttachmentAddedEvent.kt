package com.example.domain.event.messageattachment

import com.example.domain.event.DomainEvent
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.vo.DocumentId
import java.time.Instant

/**
 * Event indicating that a new attachment has been added to a message.
 */
data class MessageAttachmentAddedEvent(
    val attachmentId: DocumentId,
    val attachmentType: MessageAttachmentType,
    val attachmentUrl: String,
    override val occurredOn: Instant
) : DomainEvent
