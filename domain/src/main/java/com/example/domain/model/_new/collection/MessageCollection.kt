package com.example.domain.model.collection

import com.example.domain.model.Message
import com.example.domain.model.MessageAttachment

data class MessageCollection(
    val message: Message,
    val attachments: List<MessageAttachment>? = null
)
