package com.example.data.model.remote

import com.example.domain.model._new.enum.MessageAttachmentType
import com.example.domain.model.base.MessageAttachment
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName


/**
 * 메시지 첨부파일 정보를 나타내는 DTO 클래스
 */
data class MessageAttachmentDTO(
    @DocumentId val id: String = "",
    // "IMAGE", "FILE", "VIDEO" 등
    @get:PropertyName(ATTACHMENT_TYPE)
    val attachmentType: MessageAttachmentType = MessageAttachmentType.FILE,
    // Firebase Storage 등에 업로드된 파일의 URL
    @get:PropertyName(ATTACHMENT_URL)
    val attachmentUrl: String = "",
    @get:PropertyName(FILE_NAME)
    val fileName: String? = null,
    @get:PropertyName(FILE_SIZE)
    val fileSize: Long? = null
) {

    companion object {
        const val COLLECTION_NAME = "message_attachments"
        const val ATTACHMENT_TYPE = "attachmentType"
        const val ATTACHMENT_URL = "attachmentUrl"
        const val FILE_NAME = "fileName"
        const val FILE_SIZE = "fileSize"
    }
    fun toDomain(): MessageAttachment {
        return MessageAttachment.toDataSource(
            id = id,
            attachmentType = attachmentType,
            attachmentUrl = attachmentUrl,
            fileName = fileName,
            fileSize = fileSize,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isNew = isNew
        )
    }
}

/**
 * MessageAttachment 도메인 모델을 DTO로 변환하는 확장 함수
 * @return MessageAttachmentDTO 객체
 */
fun MessageAttachment.toDto(): MessageAttachmentDTO {
    return MessageAttachmentDTO(
        id = id,
        attachmentType = attachmentType,
        attachmentUrl = attachmentUrl,
        fileName = null, // 도메인 모델에 없는 필드는 null로 처리
        fileSize = null  // 도메인 모델에 없는 필드는 null로 처리
    )
}
