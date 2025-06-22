package com.example.domain.model.vo

import com.example.domain.model.vo.category.CategoryName
import kotlin.jvm.JvmInline

/**
 * Represents the name of a Category.
 */
@JvmInline
value class Name(val value: String) {
    init {
        require(value.isNotBlank()) { "CategoryName은 비어있을 수 없습니다." }
    }
    companion object {
    }


    fun isBlank(): Boolean {
        return this.isBlank()
    }

    fun lowercase(): String {
        return this.value.lowercase()
    }
    fun uppercase(): String {
        return this.value.uppercase()
    }
}
