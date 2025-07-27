package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Generic Outbox Entity for all SSOT operations
 * Tracks pending local changes that need to be synchronized with server
 * Part of 3-tier client-driven sync architecture
 *
 * Replaces 17 individual outbox entities with a single generic one
 */
@Entity(
    tableName = "outbox",
    indices = [
        Index(value = ["collectionName"]), // For filtering by collection type
        Index(value = ["entityId"]), // For finding operations by entity ID
        Index(value = ["localTimestamp"]), // For ordering operations chronologically
        Index(value = ["operation"]), // For filtering by operation type
        Index(value = ["retries"]) // For retry management and failure tracking
    ]
)
data class OutboxEntity(
    @PrimaryKey
    val id: String,

    /**
     * Firestore collection name (users, messages, dm_channels, etc.)
     * Used to route operations to correct sync handler
     */
    val collectionName: String,

    /**
     * Referenced entity ID from the corresponding table
     * (userId, messageId, channelId, etc.)
     */
    val entityId: String,

    /**
     * Operation type: CREATE, UPDATE, DELETE
     */
    val operation: String,

    /**
     * JSON payload with operation data (optional)
     * For UPDATE: contains field changes as delta
     * For CREATE: contains full entity data
     * For DELETE: may be null
     */
    val payload: String? = null,

    /**
     * When this operation was created locally (milliseconds since epoch)
     * Used for ordering operations and implementing exponential backoff
     */
    val localTimestamp: Long,

    /**
     * Number of failed retry attempts
     * Used for exponential backoff and eventually giving up
     */
    val retries: Int = 0
)