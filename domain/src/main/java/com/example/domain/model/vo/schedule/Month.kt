package com.example.domain.model.vo.schedule

import java.time.YearMonth

/**
 * Represents the content/description of a schedule.
 * Enforces non-blank and length constraints to prevent excessively long text.
 */
@JvmInline
value class Month(val value: Int) {
    init {
        require(value in MIN_LENGTH..MAX_LENGTH) { "Schedule month must be between $MIN_LENGTH and $MAX_LENGTH." }
    }

    companion object {
        const val MAX_LENGTH = 12
        const val MIN_LENGTH = 1

        fun from (yearMonth: YearMonth): Month {
            return Month(yearMonth.monthValue)
        }
    }
}
