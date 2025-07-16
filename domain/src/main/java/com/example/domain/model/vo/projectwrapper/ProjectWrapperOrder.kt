package com.example.domain.model.vo.projectwrapper

@JvmInline
value class ProjectWrapperOrder(val value: Int) {
    init {
        require(value >= 0) { "Order must be non-negative." }
    }
    companion object {
        val CREATE = ProjectWrapperOrder(com.example.domain.model.base.Category.MIN_CATEGORY_ORDER)
    }

}