package com.example.domain.model.sync


data class OutBoxRecord(
    val id: String,
    val stream: String,
    val aggregateId: String,
    val op: Op,
    val payload: String,
    val createdAt: Long,
    val attempt: Int = 0,
) {
    enum class Op {
        UPSERT,
        DELETE,
    }
}

data class FailedEvent(
    val id: String,
    val reason: String,
    val retryAfterMillis: Long,
)

data class PushResult(
    val successIds: List<String>,
    val failIds: List<FailedEvent>,
)

data class RemoteBatch<T>(
    val items: List<T>,
    val tombstones: List<String>,
    val nextCursor: String?,
    val hasMore: Boolean,
    val watermark: Long? = null,
)

data class ApplyOutcome(
    val success: Boolean,
    val error: Throwable? = null,
)

