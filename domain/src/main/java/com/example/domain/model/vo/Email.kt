package com.example.domain.model.vo

@JvmInline
value class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "UserId must not be blank." }
        require(value.length <= MAX_LENGTH) { "UserId cannot exceed $MAX_LENGTH characters." }
    }

    companion object {
        const val MAX_LENGTH = 128
    }
}
