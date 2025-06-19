package com.example.domain.model.vo.messageattachment

import android.net.Uri

@JvmInline
value class MessageAttachmentUrl(val value: String) {
    init {
        require(value.isNotBlank()) { "MessageAttachmentUrl must not be blank." }
    }

    companion object {
        fun fromString(value: String): MessageAttachmentUrl {
            return MessageAttachmentUrl(value)
        }
        fun fromUri(uri: Uri): MessageAttachmentUrl {
            return MessageAttachmentUrl(uri.toString())
        }
    }
}