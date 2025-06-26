package com.example.domain.model.vo

import android.util.Patterns
import com.example.core_common.constants.Constants

@JvmInline
value class Email(private val internalValue: String) {
    val value: String
        get() {
            return if (isEmpty()) {
                ""
            } else {
                internalValue
            }
        }

    init {
        require(value.isNotBlank()) { "UserId must not be blank." }
        require(value.length <= MAX_LENGTH) { "UserId cannot exceed $MAX_LENGTH characters." }
    }

    companion object {
        const val MAX_LENGTH = 128
        val EMPTY = Email(Constants.EMPTY_VALUE_STRING)
    }

    fun trim(): Email {
        return Email(value.trim())
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

    fun isEmailPatternAvailable(): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(value).matches()
    }
}
