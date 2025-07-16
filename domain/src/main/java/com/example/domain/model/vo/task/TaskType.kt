package com.example.domain.model.vo.task

/**
 * Enum representing the type of a task.
 */
enum class TaskType(val value: String) {
    CHECKLIST("checklist"),
    COMMENT("comment");

    companion object {
        fun fromValue(value: String): TaskType {
            return values().find { it.value == value } 
                ?: throw IllegalArgumentException("Unknown TaskType value: $value")
        }
    }
}