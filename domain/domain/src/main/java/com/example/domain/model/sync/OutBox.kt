package com.example.domain.model.sync

import com.example.domain.enum.EntityType
import com.example.domain.model.AggregateRoot
import com.example.domain.model.enum.OutBoxStatus
import java.time.Instant
import java.util.UUID

class OutBox<T> private constructor(
    // ================================
    // Core Identity & Content
    // ================================
    val id: String,
    val entityType: EntityType,
    val entityId: String,
    val operation: OutBoxOperation,
    val payload: OutBoxPayload<T>?,
    val baseVersion: Int,
    
    // ================================
    // Sync Configuration Options
    // ================================
    val priority: Int,
    val maxRetries: Int,
    val retryDelayMs: Long,
    val timeoutMs: Long,
    
    // ================================
    // Mutable Sync State
    // ================================
    private var status: OutBoxStatus,
    private var attempts: Int,
    val createdAt: Instant,
    private var lastAttemptAt: Instant? = null,
    private var errorMessage: String? = null
) where T : AggregateRoot {

    // ================================
    // Inner Types
    // ================================
    enum class OutBoxOperation {
        CREATE,
        UPDATE,
        DELETE;

        companion object {
            val DEFAULT = CREATE
        }

        fun isDeleteOperation(): Boolean = this == DELETE
        fun isWriteOperation(): Boolean = this in setOf(CREATE, UPDATE)
    }

    // ================================
    // State Access Methods
    // ================================
    fun getStatus(): OutBoxStatus = status
    fun getAttempts(): Int = attempts
    fun getLastAttemptAt(): Instant? = lastAttemptAt
    fun getErrorMessage(): String? = errorMessage

    // ================================
    // State Transition Methods (DDD)
    // ================================
    fun markAsProcessing() {
        require(status.canTransitionTo(OutBoxStatus.PROCESSING)) {
            "Cannot transition from $status to PROCESSING"
        }
        status = OutBoxStatus.PROCESSING
        lastAttemptAt = Instant.now()
    }

    fun markAsCompleted() {
        require(status.canTransitionTo(OutBoxStatus.COMPLETED)) {
            "Cannot transition from $status to COMPLETED"
        }
        status = OutBoxStatus.COMPLETED
        errorMessage = null
    }

    fun markAsFailed(error: String? = null) {
        require(status.canTransitionTo(OutBoxStatus.FAILED)) {
            "Cannot transition from $status to FAILED"
        }
        status = OutBoxStatus.FAILED
        attempts++
        errorMessage = error
        lastAttemptAt = Instant.now()
    }

    fun retryOperation() {
        require(canRetry()) {
            "Cannot retry: status=$status, attempts=$attempts, maxRetries=$maxRetries"
        }
        status = OutBoxStatus.PENDING
        errorMessage = null
    }

    // ================================
    // Business Logic Methods
    // ================================
    fun canRetry(): Boolean {
        return status == OutBoxStatus.FAILED && attempts < maxRetries
    }

    fun shouldWaitBeforeRetry(): Boolean {
        if (lastAttemptAt == null || status != OutBoxStatus.FAILED) return false
        
        val backoffDelay = calculateBackoffDelayMs(attempts)
        val elapsedMs = Instant.now().toEpochMilli() - lastAttemptAt!!.toEpochMilli()
        
        return elapsedMs < backoffDelay
    }

    fun isExpired(customTimeoutMs: Long = timeoutMs): Boolean {
        val elapsedMs = Instant.now().toEpochMilli() - createdAt.toEpochMilli()
        return elapsedMs > customTimeoutMs
    }

    fun isDeleteOperation(): Boolean = operation.isDeleteOperation()
    
    fun isHighPriority(): Boolean = priority <= 3
    
    fun calculateBackoffDelayMs(attemptNumber: Int): Long {
        return retryDelayMs * (1L shl attemptNumber) // Exponential backoff
    }

    companion object {
        // ================================
        // Database Schema Constants
        // ================================
        const val TABLE_NAME = "outbox"
        const val COLUMN_ID = "id"
        const val COLUMN_ENTITY_TYPE = "entity_type"
        const val COLUMN_ENTITY_ID = "entity_id"
        const val COLUMN_PRIORITY = "priority"
        const val COLUMN_MAX_RETRIES = "max_retries"
        const val COLUMN_RETRY_DELAY_MS = "retry_delay_ms"
        const val COLUMN_TIMEOUT_MS = "timeout_ms"
        const val COLUMN_OPERATION = "operation"
        const val COLUMN_PAYLOAD_JSON = "payload_json"
        const val COLUMN_BASE_VERSION = "base_version"
        const val COLUMN_STATUS = "status"
        const val COLUMN_ATTEMPTS = "attempts"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_LAST_ATTEMPT_AT = "last_attempt_at"
        const val COLUMN_ERROR_MESSAGE = "error_message"
        
        // ================================
        // Default Configuration Values
        // ================================
        const val DEFAULT_PRIORITY = 5
        const val DEFAULT_MAX_RETRIES = 3
        const val DEFAULT_RETRY_DELAY_MS = 5000L // 5 seconds
        const val DEFAULT_TIMEOUT_MS = 30000L // 30 seconds

        // ================================
        // Factory Methods
        // ================================
        fun <T> create(
            entityType: EntityType,
            entityId: String,
            operation: OutBoxOperation,
            payload: OutBoxPayload<T>,
            baseVersion: Int = 1,
            priority: Int = DEFAULT_PRIORITY,
            maxRetries: Int = DEFAULT_MAX_RETRIES,
            retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS,
            timeoutMs: Long = DEFAULT_TIMEOUT_MS
        ): OutBox<T> where T : AggregateRoot {
            return OutBox(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payload = payload,
                baseVersion = baseVersion,
                priority = priority,
                maxRetries = maxRetries,
                retryDelayMs = retryDelayMs,
                timeoutMs = timeoutMs,
                status = OutBoxStatus.DEFAULT,
                attempts = 0,
                createdAt = Instant.now()
            )
        }

        fun <T> fromDataSource(
            id: String,
            entityType: EntityType,
            entityId: String,
            operation: OutBoxOperation,
            payload: OutBoxPayload<T>,
            baseVersion: Int,
            priority: Int,  
            maxRetries: Int,
            retryDelayMs: Long,
            timeoutMs: Long,
            status: OutBoxStatus,
            attempts: Int,
            createdAt: Instant,
            lastAttemptAt: Instant? = null,
            errorMessage: String? = null
        ): OutBox<T> where T : AggregateRoot {
            return OutBox(
                id = id,
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payload = payload,
                baseVersion = baseVersion,
                priority = priority,
                maxRetries = maxRetries,
                retryDelayMs = retryDelayMs,
                timeoutMs = timeoutMs,
                status = status,
                attempts = attempts,
                createdAt = createdAt,
                lastAttemptAt = lastAttemptAt,
                errorMessage = errorMessage
            )
        }
        
        // ================================
        // Convenience Factory Methods
        // ================================
        fun <T> createHighPriority(
            entityType: EntityType,
            entityId: String,
            operation: OutBoxOperation,
            payload: OutBoxPayload<T>,
            baseVersion: Int = 1
        ): OutBox<T> where T : AggregateRoot = create(
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payload = payload,
            baseVersion = baseVersion,
            priority = 1
        )

        fun <T> createLowPriority(
            entityType: EntityType,
            entityId: String,
            operation: OutBoxOperation,
            payload: OutBoxPayload<T>,
            baseVersion: Int = 1
        ): OutBox<T> where T : AggregateRoot = create(
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payload = payload,
            baseVersion = baseVersion,
            priority = 10
        )

        fun <T> delete(
            entityType: EntityType,
            entityId: String,
            baseVersion: Int = 1,
            priority: Int = DEFAULT_PRIORITY,
            maxRetries: Int = DEFAULT_MAX_RETRIES,
            retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS,
            timeoutMs: Long = DEFAULT_TIMEOUT_MS
        ): OutBox<T> where T : AggregateRoot {
            return OutBox(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                operation = OutBoxOperation.DELETE,
                payload = null,
                baseVersion = baseVersion,
                priority = priority,
                maxRetries = maxRetries,
                retryDelayMs = retryDelayMs,
                timeoutMs = timeoutMs,
                status = OutBoxStatus.DEFAULT,
                attempts = 0,
                createdAt = Instant.now()
            )
        }
    }
}