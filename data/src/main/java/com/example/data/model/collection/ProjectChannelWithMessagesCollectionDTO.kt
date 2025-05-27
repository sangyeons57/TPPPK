package com.example.data.model.collection

data class ProjectChannelWithMessagesCollectionDTO(
    val projectChannel: ProjectChannelDocumentDTO,
    val messages: List<MessageWithAttachmentsCollectionDTO>? = null // Message와 그 첨부파일을 함께 포함
)
