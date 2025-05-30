package com.example.data.model.collection

import com.example.data.model.remote.MessageAttachmentDTO
import com.example.data.model.remote.MessageDTO

data class MessageWithAttachmentsCollectionDTO(
    val message: MessageDTO,
    val attachments: List<MessageAttachmentDTO>? = null
)
