package com.example.mapper

import com.example.data.model.remote.MessageAttachmentDTO
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.model.vo.messageattachment.MessageAttachmentUrl
import com.example.mapper.base.BaseMapper

interface MessageAttachmentMapper : BaseMapper<MessageAttachment, MessageAttachmentDTO>

class MessageAttachmentMapperImpl : MessageAttachmentMapper {
    override fun fromDto(dto: MessageAttachmentDTO): MessageAttachment {
        return MessageAttachment.fromDataSource(
            id = DocumentId(dto.id),
            attachmentType = dto.attachmentType,
            attachmentUrl = MessageAttachmentUrl(dto.attachmentUrl),
            fileName = dto.fileName?.let { MessageAttachmentFileName(it) },
            fileSize = dto.fileSize?.let { MessageAttachmentFileSize(it) },
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant(),
        )
    }

    override fun toDto(domain: MessageAttachment): MessageAttachmentDTO {
        return MessageAttachmentDTO(
            id = domain.id.value,
            attachmentType = domain.attachmentType,
            attachmentUrl = domain.attachmentUrl.value,
            fileName = domain.fileName?.value,
            fileSize = domain.fileSize?.value,
            createdAt = null,
            updatedAt = null,
        )
    }

    override fun domainToMap(domain: MessageAttachment): Map<String, Any?> {
        val currentUploadState = domain.getCurrentUploadState()
        return mapOf(
            MessageAttachment.KEY_ATTACHMENT_TYPE to domain.attachmentType.value,
            MessageAttachment.KEY_ATTACHMENT_URL to domain.attachmentUrl.value,
            MessageAttachment.KEY_FILE_NAME to domain.fileName?.value,
            MessageAttachment.KEY_FILE_SIZE to domain.fileSize?.value,
            MessageAttachment.KEY_THUMBNAIL_URL to domain.thumbnailUrl?.value,
            MessageAttachment.KEY_UPLOAD_STATUS to currentUploadState.status.value,
            MessageAttachment.KEY_UPLOAD_PROGRESS to currentUploadState.progress.value,
        )
    }

    override fun dataToMap(data: MessageAttachmentDTO): Map<String, Any?> {
        return mapOf(
            MessageAttachment.KEY_ATTACHMENT_TYPE to data.attachmentType,
            MessageAttachment.KEY_ATTACHMENT_URL to data.attachmentUrl,
            MessageAttachment.KEY_FILE_NAME to data.fileName,
            MessageAttachment.KEY_FILE_SIZE to data.fileSize,
            MessageAttachment.KEY_THUMBNAIL_URL to null,
            MessageAttachment.KEY_UPLOAD_STATUS to null,
            MessageAttachment.KEY_UPLOAD_PROGRESS to null,
        )
    }
}
