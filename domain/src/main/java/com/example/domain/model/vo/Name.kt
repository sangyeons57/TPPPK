package com.example.domain.model.vo

import kotlin.jvm.JvmInline

/**
 * Represents the name of a Category.
 */
@JvmInline
value class Name(val value: String) {
    init {
        require(value.isNotBlank()) { "CategoryName은 비어있을 수 없습니다." }
    }
}
