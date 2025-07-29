package com.example.domain.model.enum

enum class OutBoxStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    companion object {
        val DEFAULT = PENDING
        val ACTIVE_STATES = setOf(PENDING, PROCESSING)
        val TERMINAL_STATES = setOf(COMPLETED, FAILED)
    }

    fun isActive(): Boolean = this in ACTIVE_STATES
    fun isTerminal(): Boolean = this in TERMINAL_STATES
    fun canTransitionTo(newStatus: OutBoxStatus): Boolean {
        return when (this) {
            PENDING -> newStatus in setOf(PROCESSING, FAILED)
            PROCESSING -> newStatus in setOf(COMPLETED, FAILED)
            COMPLETED -> false // Terminal state
            FAILED -> newStatus == PENDING // Allow retry
        }
    }
}