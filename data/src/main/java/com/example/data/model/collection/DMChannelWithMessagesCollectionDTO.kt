package com.example.data.model.collection

import com.example.data.model.remote.DMChannelDTO

data class DMChannelWithMessagesCollectionDTO(
    val dmChannel: DMChannelDTO,
    val messages: List<MessageWithAttachmentsCollectionDTO>? = null // Message와 그 첨부파일을 함께 포함
)
