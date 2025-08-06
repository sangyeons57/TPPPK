package com.example.domain.vo.messageattachment

import java.net.URI

@JvmInline
value class MessageAttachmentUrl(val value: String) {
    init {
//        require(value.isNotBlank()) { "MessageAttachmentUrl must not be blank." }
    }

    companion object {
        fun fromString(value: String): MessageAttachmentUrl {
            return MessageAttachmentUrl(value)
        }
        fun fromUri(uri: URI): MessageAttachmentUrl {
            return MessageAttachmentUrl(uri.toString())
        }
    }
}
