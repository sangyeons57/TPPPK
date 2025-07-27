package com.example.data.mapper

import com.example.data.model.local.MessageAttachmentsEntity
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.model.vo.messageattachment.MessageAttachmentUrl
import com.example.domain.model.vo.messageattachment.MessageAttachmentThumbnailUrl
import com.example.domain.model.vo.messageattachment.MessageAttachmentUploadProgress
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.enum.MessageAttachmentUploadStatus

/**
 * MessageAttachmentsEntity와 MessageAttachment 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object MessageAttachmentsMapper {

    /**
     * MessageAttachment 도메인 모델을 MessageAttachmentsEntity로 변환
     * @param messageAttachment 변환할 MessageAttachment 도메인 모델
     * @param messageId 첨부파일이 속한 메시지 ID
     * @return MessageAttachmentsEntity
     */
    fun toEntity(
        messageAttachment: MessageAttachment,
        messageId: String
    ): MessageAttachmentsEntity {
        return MessageAttachmentsEntity(
            id = messageAttachment.id.value,
            messageId = messageId,
            attachmentType = messageAttachment.attachmentType.value,
            attachmentUrl = messageAttachment.attachmentUrl.value,
            fileName = messageAttachment.fileName?.value,
            fileSize = messageAttachment.fileSize?.value,
            thumbnailUrl = messageAttachment.thumbnailUrl?.value,
            uploadStatus = messageAttachment.uploadStatus.value,
            uploadProgress = messageAttachment.uploadProgress.value,
            createdAt = messageAttachment.createdAt,
            updatedAt = messageAttachment.updatedAt
        )
    }

    /**
     * MessageAttachmentsEntity를 MessageAttachment 도메인 모델로 변환
     * @param entity 변환할 MessageAttachmentsEntity
     * @return MessageAttachment 도메인 모델
     */
    fun toDomain(entity: MessageAttachmentsEntity): MessageAttachment {
        return MessageAttachment.fromDataSource(
            id = DocumentId(entity.id),
            attachmentType = MessageAttachmentType.fromString(entity.attachmentType),
            attachmentUrl = MessageAttachmentUrl(entity.attachmentUrl),
            fileName = entity.fileName?.let { MessageAttachmentFileName(it) },
            fileSize = entity.fileSize?.let { MessageAttachmentFileSize(it) },
            thumbnailUrl = entity.thumbnailUrl?.let { MessageAttachmentThumbnailUrl(it) },
            uploadStatus = MessageAttachmentUploadStatus.fromString(entity.uploadStatus),
            uploadProgress = MessageAttachmentUploadProgress(entity.uploadProgress),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * MessageAttachment 도메인 모델 리스트를 MessageAttachmentsEntity 리스트로 변환
     * @param messageAttachments 변환할 MessageAttachment 도메인 모델 리스트
     * @param messageId 첨부파일들이 속한 메시지 ID
     * @return MessageAttachmentsEntity 리스트
     */
    fun toEntityList(
        messageAttachments: List<MessageAttachment>,
        messageId: String
    ): List<MessageAttachmentsEntity> {
        return messageAttachments.map { toEntity(it, messageId) }
    }

    /**
     * MessageAttachmentsEntity 리스트를 MessageAttachment 도메인 모델 리스트로 변환
     * @param entities 변환할 MessageAttachmentsEntity 리스트
     * @return MessageAttachment 도메인 모델 리스트
     */
    fun toDomainList(entities: List<MessageAttachmentsEntity>): List<MessageAttachment> {
        return entities.map { toDomain(it) }
    }
}