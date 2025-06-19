package com.example.domain.model.base


import com.example.domain.event.AggregateRoot
import com.example.domain.event.messageattachment.MessageAttachmentAddedEvent
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.model.vo.messageattachment.MessageAttachmentUrl
import java.time.Instant

class MessageAttachment private constructor(
    initialAttachmentType: MessageAttachmentType, // e.g., IMAGE, FILE, VIDEO
    initialAttachmentUrl: MessageAttachmentUrl, // URL to the file in storage
    initialCreatedAt: Instant,
    initialUpdatedAt: Instant,
    initialFileName: MessageAttachmentFileName?,
    initialFileSize: MessageAttachmentFileSize?,
    override val id: DocumentId,
    override val isNew: Boolean
) : AggregateRoot() {

    val attachmentType: MessageAttachmentType = initialAttachmentType
    val attachmentUrl: MessageAttachmentUrl = initialAttachmentUrl
    val createdAt: Instant = initialCreatedAt
    val updatedAt: Instant = initialUpdatedAt
    val fileName: MessageAttachmentFileName? = initialFileName
    val fileSize: MessageAttachmentFileSize? = initialFileSize

    /**
     * A MessageAttachment's state is immutable once created.
     * There are no properties to update, so this map is empty.
     */
    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_ATTACHMENT_TYPE to this.attachmentType,
            KEY_ATTACHMENT_URL to this.attachmentUrl,
            KEY_CREATED_AT to this.createdAt,
            KEY_UPDATED_AT to this.updatedAt,
            KEY_FILE_NAME to this.fileName,
            KEY_FILE_SIZE to this.fileSize,
        )
    }

    // A MessageAttachment is immutable, so there are no business logic methods for state change.

    companion object {
        const val COLLECTION_NAME = "message_attachments"
        const val KEY_ATTACHMENT_TYPE = "attachmentType"
        const val KEY_ATTACHMENT_URL = "attachmentUrl"
        const val KEY_FILE_NAME = "fileName"
        const val KEY_FILE_SIZE = "fileSize"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"
        /**
         * Factory method for creating a new message attachment.
         */
        fun create(
            id: DocumentId,
            attachmentType: MessageAttachmentType,
            attachmentUrl: MessageAttachmentUrl,
            fileName: MessageAttachmentFileName?,
            fileSize: MessageAttachmentFileSize?
        ): MessageAttachment {
            val now = Instant.now()
            val attachment = MessageAttachment(
                initialAttachmentType = attachmentType,
                initialAttachmentUrl = attachmentUrl,
                initialCreatedAt = now,
                initialUpdatedAt = now,
                initialFileName = fileName,
                initialFileSize = fileSize,
                id = id,
                isNew = true,
            )
            attachment.pushDomainEvent(MessageAttachmentAddedEvent(
                attachmentId = id,
                attachmentType = attachmentType,
                attachmentUrl = attachmentUrl,
                occurredOn = now,
                fileName = fileName,
                fileSize = fileSize
            ))
            return attachment
        }

        /**
         * Factory method to reconstitute a MessageAttachment from a data source.
         */
        fun fromDataSource(
            id: DocumentId,
            attachmentType: MessageAttachmentType,
            attachmentUrl: MessageAttachmentUrl,
            createdAt: Instant,
            updatedAt: Instant,
            fileName: MessageAttachmentFileName?,
            fileSize: MessageAttachmentFileSize?
        ): MessageAttachment {
            return MessageAttachment(
                initialAttachmentType = attachmentType,
                initialAttachmentUrl = attachmentUrl,
                initialCreatedAt = createdAt,
                initialUpdatedAt = updatedAt,
                initialFileName = fileName,
                initialFileSize = fileSize,
                id = id,
                isNew = false,
            )
        }
    }
}
