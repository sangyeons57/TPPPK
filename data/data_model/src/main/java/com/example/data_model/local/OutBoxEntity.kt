package com.example.data_model.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.sync.OutBox

@Entity(tableName = OutBox.TABLE_NAME)
data class OutBoxEntity(
    // ================================
    // Core Identity & Content
    // ================================
    @PrimaryKey
    @ColumnInfo(name = OutBox.COLUMN_ID)
    val id: String,

    @ColumnInfo(name = OutBox.COLUMN_ENTITY_TYPE)
    val entityType: String,

    @ColumnInfo(name = OutBox.COLUMN_ENTITY_ID)
    val entityId: String,

    @ColumnInfo(name = OutBox.COLUMN_OPERATION)
    val operation: String,

    @ColumnInfo(name = OutBox.COLUMN_PAYLOAD_JSON)
    val payloadJson: String,

    @ColumnInfo(name = OutBox.COLUMN_BASE_VERSION)
    val baseVersion: Int,

    // ================================
    // Sync Configuration Options
    // ================================
    @ColumnInfo(name = OutBox.COLUMN_PRIORITY)
    val priority: Int,

    @ColumnInfo(name = OutBox.COLUMN_MAX_RETRIES)
    val maxRetries: Int,

    @ColumnInfo(name = OutBox.COLUMN_RETRY_DELAY_MS)
    val retryDelayMs: Long,

    @ColumnInfo(name = OutBox.COLUMN_TIMEOUT_MS)
    val timeoutMs: Long,

    // ================================
    // Mutable Sync State
    // ================================
    @ColumnInfo(name = OutBox.COLUMN_STATUS)
    val status: String,

    @ColumnInfo(name = OutBox.COLUMN_ATTEMPTS)
    val attempts: Int,

    @ColumnInfo(name = OutBox.COLUMN_CREATED_AT)
    val createdAt: Long, // Epoch milliseconds

    @ColumnInfo(name = OutBox.COLUMN_LAST_ATTEMPT_AT)
    val lastAttemptAt: Long? = null, // Epoch milliseconds

    @ColumnInfo(name = OutBox.COLUMN_ERROR_MESSAGE)
    val errorMessage: String? = null
)