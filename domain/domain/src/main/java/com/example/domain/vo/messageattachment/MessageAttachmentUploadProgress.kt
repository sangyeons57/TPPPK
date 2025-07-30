package com.example.domain.model.vo.messageattachment

@JvmInline
value class MessageAttachmentUploadProgress(val value: Float) {
    init {
        require(value in 0f..1f) { "Upload progress must be between 0.0 and 1.0, but was $value" }
    }

    companion object {
        fun fromPercentage(percentage: Int): MessageAttachmentUploadProgress {
            require(percentage in 0..100) { "Percentage must be between 0 and 100, but was $percentage" }
            return MessageAttachmentUploadProgress(percentage / 100f)
        }
        
        fun zero(): MessageAttachmentUploadProgress {
            return MessageAttachmentUploadProgress(0f)
        }
        
        fun complete(): MessageAttachmentUploadProgress {
            return MessageAttachmentUploadProgress(1f)
        }
    }
}