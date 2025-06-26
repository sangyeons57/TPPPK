package com.example.domain.model.vo

import com.example.core_common.constants.Constants

/**
 * Dedicated identifier for User aggregates. Separate from generic [DocumentId]
 * to allow semantic clarity and potential specialized validation.
 */
@JvmInline
value class UserId(val internalValue: String) {
    val value: String
        get() {
            return if (isEmpty()) {
                ""
            } else {
                internalValue
            }
        }

    init {
    }

    companion object {
        const val MAX_LENGTH = 128

        val UNKNOWN_USER = UserId("UNKNOWN_USER")
        val EMPTY = UserId(Constants.EMPTY_VALUE_STRING)

        fun from (value: String): UserId {
            return UserId(value)
        }
        fun from (value: DocumentId): UserId {
            return UserId(value.value)
        }

    }

    fun isBlank(): Boolean {
        return value.isBlank()
    }

    fun isNotBlank(): Boolean {
        return value.isNotBlank()
    }

    fun isEmpty(): Boolean {
        return this == EMPTY
    }

    fun isNotEmpty(): Boolean {
        return this != EMPTY
    }
}
