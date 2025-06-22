package com.example.domain.model.vo.category

import kotlin.jvm.JvmInline

/**
 * Represents the display order of a Category.
 */
@JvmInline
value class CategoryOrder(val value: Double) {
    init {
        require(value >= 0) { "CategoryOrder는 음수일 수 없습니다." }
        require(isTwoDecimalPlace(value)) { "CategoryOrder must have at most two decimal places (00.00 format)." }
    }

    companion object {
        private fun isTwoDecimalPlace(v: Double): Boolean {
            return String.format("%.2f", v).toDouble() == v
        }

        fun of(raw: Double): CategoryOrder {
            val rounded = String.format("%.2f", raw).toDouble()
            return CategoryOrder(rounded)
        }
    }
}
