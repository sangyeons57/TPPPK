package com.example.domain.model.base

import com.google.firebase.firestore.DocumentId

data class MessageAttachment(
    @DocumentId val id: String = "",
    // "IMAGE", "FILE", "VIDEO" 등
    val attachmentType: MessageAttachmentType = MessageAttachmentType.FILE,
    // Firebase Storage 등에 업로드된 파일의 URL
    val attachmentUrl: String = "",
)

