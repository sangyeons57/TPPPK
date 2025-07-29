package com.example.data_model.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.enum.EntityType
import com.example.domain.model.enum.OutBoxStatus
import com.example.domain.model.sync.OutBox
import com.example.domain.model.sync.OutBoxPayload
import java.time.Instant

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
) {

    fun toDomainModel(): OutBox {
        val entityType = EntityType.valueOf(this.entityType)
        val operation = OutBox.OutBoxOperation.valueOf(this.operation)
        val payload = OutBoxPayload(
            jsonData = this.payloadJson
        )
        val status = OutBoxStatus.valueOf(this.status)

        return OutBox.fromDataSource(
            id = this.id,
            entityType = entityType,
            entityId = this.entityId,
            operation = operation,
            payload = payload,
            baseVersion = this.baseVersion,
            priority = this.priority,
            maxRetries = this.maxRetries,
            retryDelayMs = this.retryDelayMs,
            timeoutMs = this.timeoutMs,
            status = status,
            attempts = this.attempts,
            createdAt = Instant.ofEpochMilli(this.createdAt),
            lastAttemptAt = this.lastAttemptAt?.let { Instant.ofEpochMilli(it) },
            errorMessage = this.errorMessage
        )
    }

    companion object {
        fun fromDomainModel(outBox: OutBox): OutBoxEntity {
            return OutBoxEntity(
                id = outBox.id,
                entityType = outBox.entityType.name,
                entityId = outBox.entityId,
                operation = outBox.operation.name,
                payloadJson = outBox.payload.jsonData,
                baseVersion = outBox.baseVersion,
                priority = outBox.priority,
                maxRetries = outBox.maxRetries,
                retryDelayMs = outBox.retryDelayMs,
                timeoutMs = outBox.timeoutMs,
                status = outBox.getStatus().name,
                attempts = outBox.getAttempts(),
                createdAt = outBox.createdAt.toEpochMilli(),
                lastAttemptAt = outBox.getLastAttemptAt()?.toEpochMilli(),
                errorMessage = outBox.getErrorMessage()
            )
        }
    }
}