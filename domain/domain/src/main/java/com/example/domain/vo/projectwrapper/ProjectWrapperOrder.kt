package com.example.domain.vo.projectwrapper

import com.example.domain.model.base.Category

@JvmInline
value class ProjectWrapperOrder(val value: Int) {
    init {
        require(value >= 0) { "Order must be non-negative." }
    }

    fun toDouble() : Double {
        return value.toDouble()
    }
    companion object {
        val CREATE = ProjectWrapperOrder(Category.MIN_CATEGORY_ORDER)

        fun from(value: Double) : ProjectWrapperOrder{
            return ProjectWrapperOrder(value.toInt())
        }
    }

}