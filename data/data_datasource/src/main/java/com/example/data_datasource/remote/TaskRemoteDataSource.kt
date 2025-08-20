package com.example.data_datasource.remote

import com.example.data_datasource.remote.special.DefaultDatasource
import com.example.data_datasource.remote.special.DefaultDatasourceImpl
import com.example.data_model.remote.TaskDTO
import com.example.domain.AggregateRoot
import com.example.domain.model.sync.OutBoxRecord
import com.example.domain.model.sync.PushResult
import com.example.domain.model.sync.RemoteBatch
import com.example.domain.vo.CollectionPath
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

        for (e in events) {
            try {
                val payload = JSONObject(e.payload)
                val id = payload.optString("id").takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("Missing id in payload")
                val channelIdRaw = payload.optString("channelId").takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("Missing channelId in payload")

                when (e.op) {
                    OutBoxRecord.Op.UPSERT -> {
                        val dto = TaskDTO(
                            id = id,
                            channelId = com.example.domain.vo.ChannelId(channelIdRaw).last(),
                            taskType = payload.optString("taskType", "checklist"),
                            status = TaskStatus.fromValue(
                                payload.optString(
                                    "status",
                                    TaskStatus.PENDING.value
                                )
                            ),
                            content = payload.optString("content", ""),
                            order = payload.optInt("order", 0),
                            checkedBy = payload.optString("checkedBy").takeIf { it.isNotEmpty() },
                            checkedAt = payload.optLong("checkedAt").takeIf { it > 0 }
                                ?.let { Date(it) },
                            createdAt = payload.optLong("createdAt").takeIf { it > 0 }
                                ?.let { Date(it) },
                            updatedAt = payload.optLong("updatedAt").takeIf { it > 0 }
                                ?.let { Date(it) },
                        )
                        collection.document(dto.id).set(dto).await()
                        success.add(e.id)
                    }

                    OutBoxRecord.Op.DELETE -> {
                        collection.document(id).delete().await()
                        success.add(e.id)
                    }
                }
            } catch (ex: Exception) {
                failed.add(
                    com.example.domain.model.sync.FailedEvent(
                        id = e.id,
                        reason = ex.message ?: "Unknown",
                        retryAfterMillis = 5000
                    )
                )
            }
        }
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
