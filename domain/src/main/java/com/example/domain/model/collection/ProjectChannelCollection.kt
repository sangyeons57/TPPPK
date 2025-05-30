package com.example.domain.model.collection

import com.example.domain.model.base.ProjectChannel

// MessageCollection은 같은 패키지 내에 있으므로 별도 import 불필요

data class ProjectChannelCollection(
    val projectChannel: ProjectChannel,
    val messages: List<MessageCollection>? = null // MessageWithAttachments -> MessageCollection
)
