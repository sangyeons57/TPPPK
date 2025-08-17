package com.example.domain.vo

/**
 * Represents a project identifier (Firestore document id for a project).
 * Shared across features needing project reference.
 */
@JvmInline
value class ProjectId(val value: String) {
    init {
//        require(value.isNotBlank()) { "ProjectId must not be blank." }
//        require(value.length <= MAX_LENGTH) { "ProjectId cannot exceed $MAX_LENGTH characters." }
    }

    fun toDocumentId(): DocumentId {
        return DocumentId(value)
    }

    companion object {
        const val MAX_LENGTH = 128

        fun from(value: DocumentId): ProjectId {
            return ProjectId(value.value)
        }
    }
}
