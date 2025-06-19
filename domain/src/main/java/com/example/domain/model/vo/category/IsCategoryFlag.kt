package com.example.domain.model.vo.category

import kotlin.jvm.JvmInline

/**
 * Flag indicating if the item is a Category (true) or another type (e.g., a channel, false).
 * In the original Category.kt, this was 'isCategory: Boolean = true'.
 * This VO wraps this boolean flag.
 */
@JvmInline
value class IsCategoryFlag(val value: Boolean){
    companion object {
        val BASE = IsCategoryFlag(true)
        val TRUE = IsCategoryFlag(true)
        val FALSE = IsCategoryFlag(false)
    }
}
