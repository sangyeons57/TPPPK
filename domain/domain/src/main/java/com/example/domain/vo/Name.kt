package com.example.domain.model.vo

import com.example.domain.model.vo.category.CategoryName

/**
 * Represents a general name value object.
 */
@JvmInline
value class Name(val value: String) {
    init {
//        require(value.isNotBlank()) { "Name은 비어있을 수 없습니다." }
    }
    companion object {
        val EMPTY = Name("")

        fun from(value: String): Name {
            return Name(value)
        }

        fun from(value: CategoryName): Name {
            return Name(value.value)
        }
    }

    fun isBlank(): Boolean {
        return this.value.isBlank()
    }
    fun isNotBlank(): Boolean {
        return this.value.isNotBlank()
    }

    fun trim(): Name {
        return Name(this.value.trim())
    }

    fun lowercase(): String {
        return this.value.lowercase()
    }
    fun uppercase(): String {
        return this.value.uppercase()
    }
}
