package com.example.data.model.collection

data class MessageWithAttachmentsCollectionDTO(
    val message: MessageDocumentDTO,
    val attachments: List<MessageAttachmentDocumentDTO>? = null
)
