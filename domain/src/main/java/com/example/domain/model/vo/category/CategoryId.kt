package com.example.domain.model.vo.category

import kotlin.jvm.JvmInline

/**
 * Represents the unique identifier for a Category.
 */
@JvmInline
value class CategoryId(val value: String) {
    init {
        require(value.isNotBlank()) { "CategoryId는 비어있을 수 없습니다." }
        // Assuming a similar max length as DocumentId for consistency, if not specified.
        // Or remove if no specific length constraint for CategoryId itself beyond DocumentId's general use.
        // For now, let's use a common practice for IDs.
        require(value.length <= 128) { "CategoryId는 128자를 초과할 수 없습니다." }
    }
}
