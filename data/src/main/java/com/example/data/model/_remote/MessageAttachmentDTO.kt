
package com.example.data.model._remote

import com.google.firebase.firestore.DocumentId

data class MessageAttachmentDTO(
    @DocumentId val id: String = "",
    // "IMAGE", "FILE", "VIDEO" 등
    val attachmentType: String = "FILE",
    // Firebase Storage 등에 업로드된 파일의 URL
    val attachmentUrl: String = "",
)

