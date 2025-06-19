package com.example.domain.event.messageattachment

import com.example.domain.event.DomainEvent
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.model.vo.messageattachment.MessageAttachmentUrl
import java.time.Instant

/**
 * Event indicating that a new attachment has been added to a message.
 */
data class MessageAttachmentAddedEvent(
    val attachmentId: DocumentId,
    val attachmentType: MessageAttachmentType,
    val attachmentUrl: MessageAttachmentUrl,
    val fileName: MessageAttachmentFileName?,
    val fileSize: MessageAttachmentFileSize?,
    override val occurredOn: Instant
) : DomainEvent
