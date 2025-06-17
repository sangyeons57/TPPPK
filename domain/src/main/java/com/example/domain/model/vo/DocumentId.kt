package com.example.domain.model.vo

/**
 * Generic Firestore document identifier.
 * Reusable across aggregates (User, Project, Schedule, etc.).
 */
@JvmInline
value class DocumentId(val value: String) {
    init {
        require(value.isNotBlank()) { "DocumentId must not be blank." }
        require(value.length <= MAX_LENGTH) { "DocumentId cannot exceed $MAX_LENGTH characters." }
    }

    companion object {
        const val MAX_LENGTH = 128
    }
}
