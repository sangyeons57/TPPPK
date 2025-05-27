package com.example.data.model.collection

data class DMChannelWithMessagesCollectionDTO(
    val dmChannel: DMChannelDocumentDTO,
    val messages: List<MessageWithAttachmentsCollectionDTO>? = null // Message와 그 첨부파일을 함께 포함
)
