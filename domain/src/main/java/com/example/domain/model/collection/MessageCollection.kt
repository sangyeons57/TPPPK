package com.example.domain.model.collection

import com.example.domain.model.base.Message
import com.example.domain.model.base.MessageAttachment

data class MessageCollection(
    val message: Message,
    val attachments: List<MessageAttachment>? = null
)
