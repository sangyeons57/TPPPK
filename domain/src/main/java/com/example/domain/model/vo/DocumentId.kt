package com.example.domain.model.vo

import com.google.firebase.firestore.Source

/**
 * Generic Firestore document identifier.
 * Reusable across aggregates (User, Project, Schedule, etc.).
 */
@JvmInline
value class DocumentId(val value: String) {
    init {
        // DocumentId.EMPTY_VALUE는 허용하고, 그 외의 경우에는 비어있지 않아야 함
        require(value == EMPTY_VALUE || value.isNotBlank()) { "DocumentId must not be blank unless it's an EMPTY_VALUE." }
        require(value.length <= MAX_LENGTH) { "DocumentId cannot exceed $MAX_LENGTH characters." }
    }

    companion object {
        const val MAX_LENGTH = 128
        private const val EMPTY_VALUE = "" // 빈 문자열을 특수 값으로 사용

        val EMPTY = DocumentId(EMPTY_VALUE) // 빈 DocumentId 인스턴스
    }

    /**
     * Checks if this DocumentId represents an unassigned or empty ID.
     */
    fun isAssigned(): Boolean {
        return value != EMPTY_VALUE
    }

    fun isEmpty(): Boolean {
        return value == EMPTY_VALUE
    }
}
