package com.example.domain.model.vo

import android.util.Patterns

@JvmInline
value class Email(val value: String) {
    init {
        if (isNotEmpty()) {
            require(value.isNotBlank()) { "UserId must not be blank." }
            require(value.length <= MAX_LENGTH) { "UserId cannot exceed $MAX_LENGTH characters." }
        } else {
            require(value.isEmpty()) { "UserId must be empty." }
        }
    }

    companion object {
        const val MAX_LENGTH = 128
        val EMPTY = Email("")
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
