package com.example.domain.model.vo.projectwrapper

@JvmInline
value class ProjectWrapperOrder(val value: Double) {
    init {
//        require(value > 0) { "Order must be a positive number." }
    }
    companion object {
        val CREATE = ProjectWrapperOrder(com.example.domain.model.base.Category.MIN_CATEGORY_ORDER)
    }

}