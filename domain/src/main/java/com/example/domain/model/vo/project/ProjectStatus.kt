package com.example.domain.model.vo.project

/**
 * Enum representing the status of a project.
 * This mirrors the TypeScript ProjectStatus enum exactly.
 */
enum class ProjectStatus(val value: String) {
    ACTIVE("active"),
    ARCHIVED("archived"),
    DELETED("deleted");

    companion object {
        fun fromValue(value: String): ProjectStatus {
            return values().find { it.value == value } 
                ?: throw IllegalArgumentException("Unknown ProjectStatus value: $value")
        }
    }
}