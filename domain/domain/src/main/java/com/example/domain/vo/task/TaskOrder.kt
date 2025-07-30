package com.example.domain.model.vo.task

/**
 * Value object representing the order of a task.
 */
@JvmInline
value class TaskOrder(val value: Int) {
    init {
        require(value >= 0) { "TaskOrder must be non-negative." }
    }

    companion object {
        val DEFAULT = TaskOrder(0)

        fun from(value: Int): TaskOrder {
            return TaskOrder(value)
        }
    }
}