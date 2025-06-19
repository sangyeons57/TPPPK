package com.example.domain.model.vo.messageattachment
    
@JvmInline
value class MessageAttachmentFileName(val value: String) {
    init {
        require(value.isNotBlank()) { "MessageAttachmentFileName must not be blank." }
    }
}