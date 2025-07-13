package com.example.domain.model.vo.task

/**
 * Enum representing the status of a task.
 */
enum class TaskStatus(val value: String) {
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed");

    companion object {
        fun fromValue(value: String): TaskStatus {
            return values().find { it.value == value } 
                ?: throw IllegalArgumentException("Unknown TaskStatus value: $value")
        }
    }
}