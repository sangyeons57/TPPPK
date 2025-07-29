package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Enhanced Sync Metadata Entity for managing incremental synchronization
 * Tracks last server cursor, sync timestamps, and clock skew for each collection
 * Part of 3-tier client-driven sync architecture with clock synchronization
 *
 * 🚀 Production Features:
 * - Clock skew compensation for accurate timestamp comparisons
 * - Sync health monitoring and performance tracking
 * - Configurable sync intervals per collection
 */
@Entity(
    tableName = "sync_metadata",
    indices = [
        Index(value = ["lastServerCursor"]), // For cursor-based queries
        Index(value = ["lastSuccessfulSync"]), // For sync timestamp queries
        Index(value = ["clockSkew"]) // For clock synchronization queries
    ]
)
data class SyncMetadataEntity(
    /**
     * Collection name identifier (e.g., "users", "messages", "projects")
     * Maps to Firestore collection names for consistency
     */
    @PrimaryKey
    val collectionName: String,

    /**
     * Last server cursor received from Firestore (milliseconds since epoch)
     * Corresponds to the highest updatedAt timestamp from last successful sync
     * Used for incremental sync: "get all records where updatedAt > lastServerCursor"
     */
    val lastServerCursor: Long,

    /**
     * Timestamp of last successful synchronization (milliseconds since epoch)
     * Used for tracking sync health and determining if sync is needed
     */
    val lastSuccessfulSync: Long,

    /**
     * Clock skew between client and server (milliseconds)
     * Positive value means server time is ahead of client time
     * Used to adjust local timestamps before comparing with server data
     *
     * Calculation: clockSkew = serverTime - clientTime
     * Usage: adjustedClientTime = clientTime + clockSkew
     */
    val clockSkew: Long? = null,

    /**
     * When the clock skew was last measured (milliseconds since epoch)
     * Used to determine if clock skew measurement needs refresh
     */
    val clockSkewMeasuredAt: Long? = null,

    /**
     * Number of consecutive successful synchronizations
     * Used for sync health monitoring and adaptive sync intervals
     */
    val consecutiveSuccessCount: Int = 0,

    /**
     * Number of consecutive failed synchronizations
     * Used for backoff strategies and alerting
     */
    val consecutiveFailureCount: Int = 0,

    /**
     * Average duration of successful sync operations (milliseconds)
     * Used for performance monitoring and timeout configuration
     */
    val averageSyncDurationMs: Long? = null
) {
    companion object {
        // Clock skew constants
        const val MAX_ACCEPTABLE_CLOCK_SKEW_MS = 60000L // 1 minute
        const val CLOCK_SKEW_REFRESH_INTERVAL_MS = 3600000L // 1 hour

        // Sync health constants
        const val HEALTHY_CONSECUTIVE_SUCCESS_THRESHOLD = 5
        const val UNHEALTHY_CONSECUTIVE_FAILURE_THRESHOLD = 3

        /**
         * Create initial sync metadata for a collection
         */
        fun createInitial(collectionName: String): SyncMetadataEntity {
            return SyncMetadataEntity(
                collectionName = collectionName,
                lastServerCursor = 0L,
                lastSuccessfulSync = 0L
            )
        }

        /**
         * Check if clock skew measurement is outdated
         */
        fun isClockSkewOutdated(metadata: SyncMetadataEntity): Boolean {
            val measuredAt = metadata.clockSkewMeasuredAt ?: return true
            val age = System.currentTimeMillis() - measuredAt
            return age > CLOCK_SKEW_REFRESH_INTERVAL_MS
        }

        /**
         * Adjust client timestamp using measured clock skew
         */
        fun adjustClientTime(clientTime: Long, clockSkew: Long?): Long {
            return if (clockSkew != null) {
                clientTime + clockSkew
            } else {
                clientTime
            }
        }
    }

    /**
     * Check if this collection's sync is healthy
     */
    fun isSyncHealthy(): Boolean {
        return consecutiveFailureCount < UNHEALTHY_CONSECUTIVE_FAILURE_THRESHOLD
    }

    /**
     * Check if clock skew is within acceptable range
     */
    fun hasAcceptableClockSkew(): Boolean {
        return clockSkew?.let { Math.abs(it) <= MAX_ACCEPTABLE_CLOCK_SKEW_MS } ?: true
    }

    /**
     * Get adjusted server cursor accounting for clock skew
     */
    fun getAdjustedServerCursor(): Long {
        return adjustClientTime(lastServerCursor, clockSkew)
    }
}