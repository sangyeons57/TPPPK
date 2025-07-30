package com.example.domain.model.vo.schedule

/**
 * Represents the content/description of a schedule.
 * Enforces non-blank and length constraints to prevent excessively long text.
 */
@JvmInline
value class Day(val value: Int) {
    init {
//        require(value in MIN_LENGTH..MAX_LENGTH) { "Schedule day must be between $MIN_LENGTH and $MAX_LENGTH." }
    }

    companion object {
        const val MAX_LENGTH = 31
        const val MIN_LENGTH = 1

    }
}
