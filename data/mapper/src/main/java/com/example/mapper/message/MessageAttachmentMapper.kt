package com.example.mapper.message

import com.example.data_model.remote.MessageAttachmentDTO
import com.example.domain.model.base.MessageAttachment
import com.example.domain.vo.DocumentId
import com.example.domain.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.vo.messageattachment.MessageAttachmentUrl
import com.example.mapper.DtoMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MessageAttachment 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class MessageAttachmentMapper @Inject constructor() :
    DtoMapper<MessageAttachment, MessageAttachmentDTO> {

    override fun dtoToDomain(dto: MessageAttachmentDTO): MessageAttachment {
        return MessageAttachment.fromDataSource(
            id = DocumentId(dto.id),
            attachmentType = dto.attachmentType,
            attachmentUrl = MessageAttachmentUrl.fromString(dto.attachmentUrl),
            fileName = dto.fileName?.let { MessageAttachmentFileName(it) },
            fileSize = dto.fileSize?.let { MessageAttachmentFileSize(it) },
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun domainToDto(domain: MessageAttachment): MessageAttachmentDTO {
        return MessageAttachmentDTO(
            id = domain.id.value,
            attachmentType = domain.attachmentType,
            attachmentUrl = domain.attachmentUrl.value,
            fileName = domain.fileName?.value,
            fileSize = domain.fileSize?.value,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}