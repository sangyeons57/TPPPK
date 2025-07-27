package com.example.data_model.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sync Metadata Entity for managing incremental synchronization
 * Tracks last server cursor and sync timestamps for each collection
 * Part of 3-tier client-driven sync architecture
 */
@Entity(
    tableName = "sync_metadata",
    indices = [
        Index(value = ["lastServerCursor"]), // For cursor-based queries
        Index(value = ["lastSuccessfulSync"]) // For sync timestamp queries
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
    val lastSuccessfulSync: Long
)