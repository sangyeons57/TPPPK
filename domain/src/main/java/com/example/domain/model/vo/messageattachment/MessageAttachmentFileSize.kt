package com.example.domain.model.vo.messageattachment   

/**
 * Generic Firestore document identifier.
 * Reusable across aggregates (User, Project, Schedule, etc.).
 */
@JvmInline
value class MessageAttachmentFileSize(val value: Long) {
    init {
//        require(value >= 0) { "MessageAttachmentFileSize must be a non-negative value." }
    }
}