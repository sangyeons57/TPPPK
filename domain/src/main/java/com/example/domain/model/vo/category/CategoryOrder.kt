package com.example.domain.model.vo.category

/**
 * Represents the display order of a Category.
 */
@JvmInline
value class CategoryOrder(val value: Double) {
    init {
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
