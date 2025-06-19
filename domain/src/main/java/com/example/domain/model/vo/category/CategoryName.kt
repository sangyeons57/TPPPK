package com.example.domain.model.vo.category

import com.example.domain.model.base.Category
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.UserId
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

    fun getName() : Name{
        return Name(value)
    }

    companion object {
        val NO_CATEGORY_NAME = CategoryName("카테고리 없음")

        fun from(name: Name): CategoryName {
            return CategoryName(name.value)
        }
    }
}
