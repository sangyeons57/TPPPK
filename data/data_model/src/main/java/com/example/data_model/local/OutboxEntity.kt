package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Enhanced Outbox Entity for production-level SSOT operations
 * Tracks pending local changes that need to be synchronized with server
 * Part of 3-tier client-driven sync architecture with idempotency and ordering guarantees
 *
 * 🚀 Production Features:
 * - Idempotency keys for duplicate prevention
 * - Ordering keys for sequence guarantees
 * - Status tracking for lifecycle management
 * - Retry scheduling with exponential backoff
 * - Error tracking for debugging
 *
 * Replaces 17 individual outbox entities with a single generic one
 */
@Entity(
    tableName = "outbox",
    indices = [
        Index(value = ["collectionName"]), // For filtering by collection type
        Index(value = ["documentId"]), // For finding operations by entity ID
        Index(value = ["status"]), // For filtering by processing status
        Index(value = ["orderingKey"]), // For ordering operations
        Index(value = ["scheduledAt"]), // For retry scheduling
        Index(value = ["localTimestamp"]), // For chronological ordering
        Index(value = ["operation"]), // For filtering by operation type
        Index(value = ["retries"]) // For retry management and failure tracking
    ]
)
data class OutboxEntity(
    @PrimaryKey
    val id: String,

    /**
     * Idempotency key for duplicate prevention
     * - Same operation on same entity should have same key
     * - Prevents duplicate processing in distributed scenarios
     * - Format: "${collectionName}_${documentId}_${operation}_${contentHash}"
     */
    val idempotencyKey: String,

    /**
     * Ordering key for sequence guarantees
     * - Ensures operations on same entity are processed in order
     * - Format: "${collectionName}_${documentId}" or custom grouping
     * - SyncManager processes by orderingKey sequentially
     */
    val orderingKey: String,

    /**
     * Firestore collection name (users, messages, dm_channels, etc.)
     * Used to route operations to correct sync handler
     */
    val collectionName: String,

    /**
     * Referenced entity ID from the corresponding table
     * (userId, messageId, channelId, etc.)
     */
    val documentId: String,

    /**
     * Operation type: CREATE, UPDATE, DELETE
     */
    val operation: String,

    /**
     * Processing status: PENDING, PROCESSING, COMPLETED, FAILED
     * Used by SyncManager for lease/ack pattern
     */
    val status: String,

    /**
     * JSON payload with operation data (optional)
     * For UPDATE: contains field changes as delta
     * For CREATE: contains full entity data
     * For DELETE: may be null
     */
    val payload: String? = null,

    /**
     * When this operation was created locally (milliseconds since epoch)
     * Used for ordering operations and age-based cleanup
     */
    val localTimestamp: Long,

    /**
     * When this operation should be processed next (milliseconds since epoch)
     * Used for exponential backoff and retry scheduling
     * NULL means process immediately
     */
    val scheduledAt: Long? = null,

    /**
     * Number of failed retry attempts
     * Used for exponential backoff and eventually giving up
     */
    val retries: Int = 0,

    /**
     * Maximum number of retry attempts before giving up
     * Configurable per operation type or use global default
     */
    val maxRetries: Int = 3,

    /**
     * Last error message from failed processing attempt
     * Used for debugging and user-facing error messages
     */
    val errorMessage: String? = null,

    /**
     * When this operation was last processed (milliseconds since epoch)
     * Used for debugging and performance monitoring
     */
    val lastProcessedAt: Long? = null
) {
    companion object {
        // Default status values
        const val STATUS_PENDING = "PENDING"
        const val STATUS_PROCESSING = "PROCESSING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"

        // Default retry configuration
        const val DEFAULT_MAX_RETRIES = 3
        const val EXPONENTIAL_BACKOFF_BASE_MS = 1000L // 1 second
        const val MAX_BACKOFF_MS = 300000L // 5 minutes

        /**
         * Generate idempotency key for an operation
         */
        fun generateIdempotencyKey(
            collectionName: String,
            documentId: String,
            operation: String,
            contentHash: String? = null
        ): String {
            val base = "${collectionName}_${documentId}_${operation}"
            return if (contentHash != null) {
                "${base}_${contentHash}"
            } else {
                base
            }
        }

        /**
         * Generate ordering key for sequential processing
         */
        fun generateOrderingKey(
            collectionName: String,
            documentId: String
        ): String {
            return "${collectionName}_${documentId}"
        }

        /**
         * Calculate next retry time using exponential backoff
         */
        fun calculateNextRetry(retries: Int): Long {
            val backoffMs =
                (EXPONENTIAL_BACKOFF_BASE_MS * Math.pow(2.0, retries.toDouble())).toLong()
            val cappedBackoff = minOf(backoffMs, MAX_BACKOFF_MS)
            return System.currentTimeMillis() + cappedBackoff
        }
    }
}