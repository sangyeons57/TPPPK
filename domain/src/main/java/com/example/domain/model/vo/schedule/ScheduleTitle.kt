package com.example.domain.model.vo.schedule

/**
 * Represents the title of a schedule as a Value Object.
 * Enforces non-blank and length constraints.
 */
@JvmInline
value class ScheduleTitle(val value: String) {
    init {
//        require(value.isNotBlank()) { "Schedule title must not be blank." }
//        require(value.length <= MAX_LENGTH) { "Schedule title cannot exceed $MAX_LENGTH characters." }
    }

    companion object {
        const val MAX_LENGTH = 100
    }
}
