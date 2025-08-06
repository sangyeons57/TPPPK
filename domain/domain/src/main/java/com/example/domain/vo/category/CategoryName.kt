package com.example.domain.vo.category

import com.example.domain.vo.Name

/**
 * Represents the name of a Category.
 */
@JvmInline
value class CategoryName(val value: String) {
    init {
        require(value.isNotBlank()) { "Category name cannot be blank" }
        require(value.length <= MAX_LENGTH) { "Category name cannot exceed $MAX_LENGTH characters" }
        require(value.trim() == value) { "Category name cannot have leading or trailing whitespace" }
    }

    fun getName() : Name{
        return Name(value)
    }

    fun isBlank(): Boolean {
        return value.isBlank()
    }

    fun trim(): CategoryName {
        return CategoryName(value.trim())
    }

    companion object {
        const val MAX_LENGTH = 50
        val NO_CATEGORY_NAME = CategoryName("카테고리 없음")

        fun from(name: Name): CategoryName {
            return CategoryName(name.value)
        }

        fun from(name: String): CategoryName {
            return CategoryName(name)
        }
    }
}
