package com.example.domain.model.enum

enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED;

    companion object {
        val DEFAULT = SYNCED
        val NEEDS_SYNC = setOf(PENDING, FAILED)
    }

    fun needsSync(): Boolean = this in NEEDS_SYNC
    fun isInProgress(): Boolean = this == SYNCING
    fun isCompleted(): Boolean = this == SYNCED
    fun hasFailed(): Boolean = this == FAILED
}