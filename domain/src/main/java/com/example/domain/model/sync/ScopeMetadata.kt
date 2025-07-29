package com.example.domain.model.sync

import java.time.Instant

class ScopeMetadata private constructor(
    val key: String,
    private var lastCursor: Instant,
    private var lastSyncedAt: Instant,
    private var syncCount: Long = 0,
    private var errorCount: Long = 0,
    private var lastErrorMessage: String? = null
) {

    fun getLastCursor(): Instant = lastCursor
    fun getLastSyncedAt(): Instant = lastSyncedAt
    fun getSyncCount(): Long = syncCount
    fun getErrorCount(): Long = errorCount
    fun getLastErrorMessage(): String? = lastErrorMessage

    fun updateCursor(newCursor: Instant) {
        require(newCursor.isAfter(lastCursor) || newCursor == lastCursor) {
            "New cursor must not be before current cursor"
        }
        this.lastCursor = newCursor
        this.lastSyncedAt = Instant.now()
        this.syncCount++
        this.lastErrorMessage = null // Clear error on successful sync
    }

    fun recordError(errorMessage: String) {
        this.errorCount++
        this.lastErrorMessage = errorMessage
    }

    fun resetErrorCount() {
        this.errorCount = 0
        this.lastErrorMessage = null
    }

    fun hasRecentError(withinMinutes: Long = 5): Boolean {
        return lastErrorMessage != null && 
               Instant.now().minusSeconds(withinMinutes * 60).isBefore(lastSyncedAt)
    }

    companion object {
        const val TABLE_NAME = "scope_metadata"
        const val COLUMN_KEY = "key"
        const val COLUMN_LAST_CURSOR = "last_cursor"
        const val COLUMN_LAST_SYNCED_AT = "last_synced_at"
        const val COLUMN_SYNC_COUNT = "sync_count"
        const val COLUMN_ERROR_COUNT = "error_count"
        const val COLUMN_LAST_ERROR_MESSAGE = "last_error_message"
        
        fun create(
            key: String,
            initialCursor: Instant = Instant.EPOCH
        ): ScopeMetadata {
            require(key.isNotBlank()) { "Key cannot be blank" }
            
            return ScopeMetadata(
                key = key,
                lastCursor = initialCursor,
                lastSyncedAt = Instant.now(),
                syncCount = 0,
                errorCount = 0
            )
        }

        fun fromDataSource(
            key: String,
            lastCursor: Instant,
            lastSyncedAt: Instant,
            syncCount: Long = 0,
            errorCount: Long = 0,
            lastErrorMessage: String? = null
        ): ScopeMetadata {
            return ScopeMetadata(
                key = key,
                lastCursor = lastCursor,
                lastSyncedAt = lastSyncedAt,
                syncCount = syncCount,
                errorCount = errorCount,
                lastErrorMessage = lastErrorMessage
            )
        }
    }
}