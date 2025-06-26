package com.example.data.model.remote

import com.example.core_common.util.DateTimeUtil
import com.example.data.model.DTO
import com.example.domain.model.base.MessageAttachment
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.model.vo.messageattachment.MessageAttachmentUrl
import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp
import com.example.domain.model.vo.DocumentId as VODocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp


/**
 * 메시지 첨부파일 정보를 나타내는 DTO 클래스
 */
data class MessageAttachmentDTO(
    @DocumentId override val id: String = "",
    // "IMAGE", "FILE", "VIDEO" 등
    private val attachmentTypeParam: MessageAttachmentType = MessageAttachmentType.FILE,
    // Firebase Storage 등에 업로드된 파일의 URL
    @get:PropertyName(ATTACHMENT_URL)
    val attachmentUrl: String = "",
    @get:PropertyName(FILE_NAME)
    val fileName: String? = null,
    @get:PropertyName(FILE_SIZE)
    val fileSize: Long? = null,
    @get:PropertyName(CREATED_AT)
    @ServerTimestamp val createdAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp(),
    @get:PropertyName(UPDATED_AT)
    @ServerTimestamp val updatedAt: Timestamp = DateTimeUtil.nowFirebaseTimestamp()
) : DTO {
    
    @get:PropertyName(ATTACHMENT_TYPE)
    @set:PropertyName(ATTACHMENT_TYPE)
    var _attachmentTypeString: String = attachmentTypeParam.value
        private set
    
    val attachmentType: MessageAttachmentType
        get() = MessageAttachmentType.fromString(_attachmentTypeString)

    companion object {
        const val COLLECTION_NAME = MessageAttachment.COLLECTION_NAME
        const val ATTACHMENT_TYPE = MessageAttachment.KEY_ATTACHMENT_TYPE
        const val ATTACHMENT_URL = MessageAttachment.KEY_ATTACHMENT_URL
        const val FILE_NAME = MessageAttachment.KEY_FILE_NAME
        const val FILE_SIZE = MessageAttachment.KEY_FILE_SIZE
        const val CREATED_AT = MessageAttachment.KEY_CREATED_AT
        const val UPDATED_AT = MessageAttachment.KEY_UPDATED_AT

        fun from(messageAttachment: MessageAttachment): MessageAttachmentDTO {
            return MessageAttachmentDTO(
                id = messageAttachment.id.value,
                attachmentTypeParam = messageAttachment.attachmentType,
                attachmentUrl = messageAttachment.attachmentUrl.value,
                fileName = messageAttachment.fileName?.value,
                fileSize = messageAttachment.fileSize?.value,
                createdAt = DateTimeUtil.instantToFirebaseTimestamp(messageAttachment.createdAt),
                updatedAt = DateTimeUtil.instantToFirebaseTimestamp(messageAttachment.updatedAt),
            )
        }
    }
    override fun toDomain(): MessageAttachment {
        return MessageAttachment.fromDataSource(
            id = VODocumentId(id),
            attachmentType = attachmentType,
            attachmentUrl = MessageAttachmentUrl(attachmentUrl),
            fileName = fileName?.let { MessageAttachmentFileName(it) },
            fileSize = fileSize?.let { MessageAttachmentFileSize(it) },
            createdAt = DateTimeUtil.firebaseTimestampToInstant(createdAt),
            updatedAt = DateTimeUtil.firebaseTimestampToInstant(updatedAt),
        )
    }
}

/**
 * MessageAttachment 도메인 모델을 DTO로 변환하는 확장 함수
 * @return MessageAttachmentDTO 객체
 */
fun MessageAttachment.toDto(): MessageAttachmentDTO {
    return MessageAttachmentDTO(
        id = id.value,
        attachmentTypeParam = attachmentType,
        attachmentUrl = attachmentUrl.value,
        fileName = fileName?.value,
        fileSize = fileSize?.value,
        createdAt = DateTimeUtil.instantToFirebaseTimestamp(createdAt),
        updatedAt = DateTimeUtil.instantToFirebaseTimestamp(updatedAt),
    )
}
