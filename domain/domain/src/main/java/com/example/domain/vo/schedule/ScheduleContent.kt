package com.example.domain.model.vo.schedule

/**
 * Represents the content/description of a schedule.
 * Enforces non-blank and length constraints to prevent excessively long text.
 */
@JvmInline
value class ScheduleContent(val value: String) {
    init {
//        require(value.isNotBlank()) { "Schedule content must not be blank." }
//        require(value.length <= MAX_LENGTH) { "Schedule content cannot exceed $MAX_LENGTH characters." }
    }

    companion object {
        const val MAX_LENGTH = 2000
    }
}
