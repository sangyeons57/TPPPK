package com.example.domain.model.vo.category

import kotlin.jvm.JvmInline

/**
 * Represents the display order of a Category.
 */
@JvmInline
value class CategoryOrder(val value: Double) {
    // No specific validation for Double mentioned in prompt, keeping it simple.
    // init { require(value >= 0) { "CategoryOrder는 음수일 수 없습니다." } } // Example if needed
}
