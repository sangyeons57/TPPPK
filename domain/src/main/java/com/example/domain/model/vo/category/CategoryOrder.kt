package com.example.domain.model.vo.category

/**
 * Represents the display order of a Category.
 */
@JvmInline
value class CategoryOrder(val value: Int) {
    init {
        require(value >= 0) { "CategoryOrder must be non-negative" }
    }

    companion object {
        fun of(raw: Int): CategoryOrder {
            return CategoryOrder(raw)
        }
        
        fun fromDouble(raw: Double): CategoryOrder {
            return CategoryOrder(raw.toInt())
        }
    }
}
