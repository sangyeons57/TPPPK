package com.example.domain.model.vo.task

/**
 * Value object representing the order of a task container.
 */
@JvmInline
value class TaskContainerOrder(val value: Int) {
    init {
        require(value >= 0) { "TaskContainerOrder must be non-negative." }
    }

    companion object {
        val DEFAULT = TaskContainerOrder(0)

        fun from(value: Int): TaskContainerOrder {
            return TaskContainerOrder(value)
        }
    }
}