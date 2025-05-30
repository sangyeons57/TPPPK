package com.example.data.model.remote

import com.example.domain.model._new.enum.MessageAttachmentType
import com.example.domain.model.base.MessageAttachment
import com.google.firebase.firestore.DocumentId

/**
 * 메시지 첨부파일 정보를 나타내는 DTO 클래스
 */
data class MessageAttachmentDTO(
    @DocumentId val id: String = "",
    // "IMAGE", "FILE", "VIDEO" 등
    val attachmentType: String = "FILE",
    // Firebase Storage 등에 업로드된 파일의 URL
    val attachmentUrl: String = "",
    val fileName: String? = null,
    val fileSize: Long? = null
) {
    /**
     * DTO를 도메인 모델로 변환
     * @return MessageAttachment 도메인 모델
     */
    fun toDomain(): MessageAttachment {
        return MessageAttachment(
            id = id,
            attachmentType = try {
                MessageAttachmentType.valueOf(attachmentType.uppercase())
            } catch (e: Exception) {
                MessageAttachmentType.FILE
            },
            attachmentUrl = attachmentUrl
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
        attachmentType = attachmentType.name.lowercase(),
        attachmentUrl = attachmentUrl,
        fileName = null, // 도메인 모델에 없는 필드는 null로 처리
        fileSize = null  // 도메인 모델에 없는 필드는 null로 처리
    )
}
