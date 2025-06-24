package com.example.domain.model.vo

import kotlin.jvm.JvmInline

/**
 * Represents a general name value object.
 */
@JvmInline
value class Name(val value: String) {
    init {
        require(value.isNotBlank()) { "Name은 비어있을 수 없습니다." }
    }
    companion object {
    }

    fun isBlank(): Boolean {
        return this.value.isBlank()
    }

    fun lowercase(): String {
        return this.value.lowercase()
    }
    fun uppercase(): String {
        return this.value.uppercase()
    }
}
