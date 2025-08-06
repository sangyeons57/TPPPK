package com.example.domain.vo.messageattachment
    
@JvmInline
value class MessageAttachmentFileName(val value: String) {
    init {
//        require(value.isNotBlank()) { "MessageAttachmentFileName must not be blank." }
    }
}