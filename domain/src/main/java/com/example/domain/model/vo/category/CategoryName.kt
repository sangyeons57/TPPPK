package com.example.domain.model.vo.category

import com.example.domain.model.vo.Name

/**
 * Represents the name of a Category.
 */
@JvmInline
value class CategoryName(val value: String) {
    init {
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
        val NO_CATEGORY_NAME = CategoryName("카테고리 없음")

        fun from(name: Name): CategoryName {
            return CategoryName(name.value)
        }

        fun from(name: String): CategoryName {
            return CategoryName(name)
        }
    }
}
