package com.example.domain.model.vo.category

import kotlin.jvm.JvmInline

/**
 * Represents the name of a Category.
 */
@JvmInline
value class CategoryName(val value: String) {
    init {
        require(value.isNotBlank()) { "CategoryName은 비어있을 수 없습니다." }
        require(value.length <= 100) { "CategoryName은 100자를 초과할 수 없습니다." }
    }
}
