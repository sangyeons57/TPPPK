package com.example.data_datasource.remote

import com.example.data_datasource.remote.special.DefaultDatasource
import com.example.data_datasource.remote.special.DefaultDatasourceImpl
import com.example.data_model.remote.TaskDTO
import com.example.domain.AggregateRoot
import com.example.domain.model.sync.OutBoxRecord
import com.example.domain.model.sync.PushResult
import com.example.domain.model.sync.RemoteBatch
import com.example.domain.vo.task.TaskStatus
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 태스크 정보에 접근하기 위한 인터페이스입니다.
 */
interface TaskRemoteDataSource : DefaultDatasource<TaskDTO> {
    suspend fun push(events: List<OutBoxRecord>): PushResult
    suspend fun pullSince(cursor: String?, limit: Int): RemoteBatch<TaskDTO>
}

@Singleton
class TaskRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<TaskDTO>(firestore), TaskRemoteDataSource {
    override val dtoClass = TaskDTO::class.java

    private fun parseCursor(cursor: String?): Pair<Long?, String?> {
        if (cursor.isNullOrEmpty()) return null to null
        val p = cursor.split(":", limit = 2)
        return p.getOrNull(0)?.toLongOrNull() to p.getOrNull(1)
    }

    override suspend fun push(events: List<OutBoxRecord>): PushResult {
        val success = mutableListOf<String>()
        val failed = mutableListOf<com.example.domain.model.sync.FailedEvent>()
        val TAG = "TaskRemoteDataSource" // Gemini-added TAG

        android.util.Log.d(TAG, "Starting push for ${events.size} events...")

        for (e in events) {
            try {
                val payload = JSONObject(e.payload)
                val id = e.aggregateId
                val channelIdRaw = payload.optString("channelId").takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("Missing channelId in payload")

                android.util.Log.d(TAG, "Processing event op=${e.op}, id=$id")

                when (e.op) {
                    OutBoxRecord.Op.UPSERT -> {
                        val dto = TaskDTO(
                            id = id,
                            // Preserve full composite channelId (no truncation)
                            channelId = channelIdRaw,
                            taskType = payload.optString("taskType", "checklist"),
                            status = TaskStatus.fromValue(
                                payload.optString(
                                    "status",
                                    TaskStatus.PENDING.value
                                )
                            ),
                            content = payload.optString("content", ""),
                            order = payload.optInt("order", 0),
                            // Ensure JSON null becomes Kotlin null (avoid string "null")
                            checkedBy = if (payload.isNull("checkedBy")) null
                            else payload.optString("checkedBy").takeIf { it.isNotBlank() },
                            checkedAt = payload.optLong("checkedAt").takeIf { it > 0 }
                                ?.let { Date(it) },
                            createdAt = payload.optLong("createdAt").takeIf { it > 0 }
                                ?.let { Date(it) },
                            updatedAt = payload.optLong("updatedAt").takeIf { it > 0 }
                                ?.let { Date(it) },
                        )
                        android.util.Log.d(
                            TAG,
                            "Attempting to SET document: ${collection.path}/$id"
                        )
                        collection.document(dto.id).set(dto).await()
                        android.util.Log.i(TAG, "SUCCESS: SET document ${collection.path}/$id")
                        success.add(e.id)
                    }

                    OutBoxRecord.Op.DELETE -> {
                        android.util.Log.d(
                            TAG,
                            "Attempting to DELETE document: ${collection.path}/$id"
                        )
                        collection.document(id).delete().await()
                        android.util.Log.i(TAG, "SUCCESS: DELETE document ${collection.path}/$id")
                        success.add(e.id)
                    }
                }
            } catch (ex: Exception) {
                // Gemini-added CRITICAL logging for failures
                val reason = ex.message ?: "Unknown reason"
                android.util.Log.e(
                    TAG,
                    "FAILURE: Failed to push event for id=${e.aggregateId}. Reason: $reason",
                    ex
                )
                failed.add(
                    com.example.domain.model.sync.FailedEvent(
                        id = e.id,
                        reason = reason,
                        retryAfterMillis = 5000
                    )
                )
            }
        }
        android.util.Log.d(TAG, "Push finished. Success: ${success.size}, Failed: ${failed.size}")
        return PushResult(successIds = success, failIds = failed)
    }

    override suspend fun pullSince(cursor: String?, limit: Int): RemoteBatch<TaskDTO> {
        val (lastTs, lastId) = parseCursor(cursor)

        var q: Query = collection
            .orderBy(AggregateRoot.KEY_UPDATED_AT, Query.Direction.ASCENDING)
            .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
            .limit(limit.toLong())

        if (lastTs != null && lastId != null) {
            q = q.startAfter(Date(lastTs), lastId)
        }

        val snap = q.get().await()
        val items = snap.toObjects(TaskDTO::class.java)

        val nextCursor = if (items.isNotEmpty()) {
            val last = items.last()
            val ts = last.updatedAt?.time ?: 0L
            if (ts > 0) "$ts:${last.id}" else null
        } else null

        return RemoteBatch(
            items = items,
            tombstones = emptyList(),
            nextCursor = nextCursor,
            hasMore = items.size.toLong() == limit.toLong(),
            watermark = null
        )
    }
}
