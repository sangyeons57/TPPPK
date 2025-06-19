package com.example.domain.model.vo.projectwrapper

@JvmInline
value class ProjectWrapperOrder(val value: Double) {
    init {
        require(value > 0) { "Order must be a positive number." }
    }
}