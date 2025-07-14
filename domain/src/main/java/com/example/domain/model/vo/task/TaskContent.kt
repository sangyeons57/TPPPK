package com.example.domain.model.vo.task

/**
 * Value object representing task content.
 */
@JvmInline
value class TaskContent(val value: String) {
    init {
        require(value.length <= MAX_LENGTH) { "TaskContent cannot exceed $MAX_LENGTH characters." }
    }

    companion object {
        const val MAX_LENGTH = 2000

        val EMPTY = TaskContent("")

        fun from(value: String): TaskContent {
            return TaskContent(value.trim())
        }
    }

    fun isEmpty(): Boolean {
        return value.isEmpty()
    }

    fun isNotEmpty(): Boolean {
        return value.isNotEmpty()
    }

    fun isBlank(): Boolean {
        return value.isBlank()
    }

    fun isNotBlank(): Boolean {
        return value.isNotBlank()
    }
}