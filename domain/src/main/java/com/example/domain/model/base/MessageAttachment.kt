package com.example.domain.model.base


import com.example.domain.model.AggregateRoot
import com.example.domain.event.messageattachment.MessageAttachmentAddedEvent
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.model.vo.messageattachment.MessageAttachmentUrl
import java.time.Instant
import com.example.core_common.util.DateTimeUtil

class MessageAttachment private constructor(
    initialAttachmentType: MessageAttachmentType, // e.g., IMAGE, FILE, VIDEO
    initialAttachmentUrl: MessageAttachmentUrl, // URL to the file in storage
    initialFileName: MessageAttachmentFileName?,
    initialFileSize: MessageAttachmentFileSize?,
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    val attachmentType: MessageAttachmentType = initialAttachmentType
    val attachmentUrl: MessageAttachmentUrl = initialAttachmentUrl
    val fileName: MessageAttachmentFileName? = initialFileName
    val fileSize: MessageAttachmentFileSize? = initialFileSize

    /**
     * A MessageAttachment's state is immutable once created.
     * There are no properties to update, so this map is empty.
     */
    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_ATTACHMENT_TYPE to this.attachmentType.value,
            KEY_ATTACHMENT_URL to this.attachmentUrl.value,
            KEY_CREATED_AT to this.createdAt,
            KEY_UPDATED_AT to this.updatedAt,
            KEY_FILE_NAME to this.fileName?.value,
            KEY_FILE_SIZE to this.fileSize?.value,
        )
    }

    // A MessageAttachment is immutable, so there are no business logic methods for state change.

    companion object {
        const val COLLECTION_NAME = "message_attachments"
        const val KEY_ATTACHMENT_TYPE = "attachmentType"
        const val KEY_ATTACHMENT_URL = "attachmentUrl"
        const val KEY_FILE_NAME = "fileName"
        const val KEY_FILE_SIZE = "fileSize"
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
            val attachment = MessageAttachment(
                initialAttachmentType = attachmentType,
                initialAttachmentUrl = attachmentUrl,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                initialFileName = fileName,
                initialFileSize = fileSize,
                id = id,
                isNew = true,
            )
            return attachment
        }

        /**
         * Factory method to reconstitute a MessageAttachment from a data source.
         */
        fun fromDataSource(
            id: DocumentId,
            attachmentType: MessageAttachmentType,
            attachmentUrl: MessageAttachmentUrl,
            createdAt: Instant?,
            updatedAt: Instant?,
            fileName: MessageAttachmentFileName?,
            fileSize: MessageAttachmentFileSize?
        ): MessageAttachment {
            return MessageAttachment(
                initialAttachmentType = attachmentType,
                initialAttachmentUrl = attachmentUrl,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                initialFileName = fileName,
                initialFileSize = fileSize,
                id = id,
                isNew = false,
            )
        }
    }
}
