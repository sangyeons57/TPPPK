package com.example.domain.model.vo.messageattachment

import java.net.URI

@JvmInline
value class MessageAttachmentThumbnailUrl(val value: String) {
    init {
        // 썸네일 URL은 선택적이므로 빈 문자열도 허용
    }

    companion object {
        fun fromString(value: String): MessageAttachmentThumbnailUrl {
            return MessageAttachmentThumbnailUrl(value)
        }
        fun fromUri(uri: URI): MessageAttachmentThumbnailUrl {
            return MessageAttachmentThumbnailUrl(uri.toString())
        }
        fun empty(): MessageAttachmentThumbnailUrl {
            return MessageAttachmentThumbnailUrl("")
        }
    }
}