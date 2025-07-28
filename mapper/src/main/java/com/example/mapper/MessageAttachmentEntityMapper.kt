package com.example.mapper

import com.example.data_core.model.local.MessageAttachmentsEntity
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.enum.MessageAttachmentUploadStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.model.vo.messageattachment.MessageAttachmentUploadProgress
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface MessageAttachmentEntityMapper : BaseEntityMapper<MessageAttachment, MessageAttachmentsEntity>

class MessageAttachmentEntityMapperImpl @Inject constructor() : MessageAttachmentEntityMapper {
    override fun toDomain(entity: MessageAttachmentsEntity): MessageAttachment {
        return MessageAttachment.fromDataSource(
            id = DocumentId(entity.id),
            messageId = DocumentId(entity.messageId),
            fileName = MessageAttachmentFileName(entity.fileName),
            fileSize = MessageAttachmentFileSize(entity.fileSize),
            fileType = MessageAttachmentType.valueOf(entity.fileType),
            fileUrl = entity.fileUrl,
            thumbnailUrl = entity.thumbnailUrl,
            uploadStatus = MessageAttachmentUploadStatus.valueOf(entity.uploadStatus),
            uploadProgress = MessageAttachmentUploadProgress(entity.uploadProgress),
            errorMessage = entity.errorMessage,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override fun toEntity(domain: MessageAttachment): MessageAttachmentsEntity {
        return MessageAttachmentsEntity(
            id = domain.id.value,
            messageId = domain.messageId.value,
            fileName = domain.fileName.value,
            fileSize = domain.fileSize.value,
            fileType = domain.fileType.name,
            fileUrl = domain.fileUrl,
            thumbnailUrl = domain.thumbnailUrl,
            uploadStatus = domain.uploadStatus.name,
            uploadProgress = domain.uploadProgress.value,
            errorMessage = domain.errorMessage,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
