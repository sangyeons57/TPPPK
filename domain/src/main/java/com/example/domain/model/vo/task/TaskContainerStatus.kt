package com.example.domain.model.vo.task

/**
 * Enum representing the status of a task container.
 */
enum class TaskContainerStatus(val value: String) {
    ACTIVE("active"),
    ARCHIVED("archived"),
    DELETED("deleted");

    companion object {
        fun fromValue(value: String): TaskContainerStatus {
            return values().find { it.value == value } 
                ?: throw IllegalArgumentException("Unknown TaskContainerStatus value: $value")
        }
    }
}