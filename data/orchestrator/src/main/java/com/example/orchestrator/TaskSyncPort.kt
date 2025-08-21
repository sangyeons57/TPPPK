package com.example.orchestrator

import android.util.Log
import com.example.core_common.constants.ChannelConstants
import com.example.data_datasource.remote.TaskRemoteDataSource
import com.example.data_model.local.OutboxDao
import com.example.data_model.local.TaskDao
import com.example.data_model.local.toModel
import com.example.domain.model.base.Task
import com.example.domain.model.sync.ApplyOutcome
import com.example.domain.model.sync.ConflictResolver
import com.example.domain.model.sync.FailedEvent
import com.example.domain.model.sync.OutBoxRecord
import com.example.domain.model.sync.PushResult
import com.example.domain.model.sync.RemoteBatch
import com.example.domain.model.sync.SyncPort
import com.example.domain.vo.ChannelId
import com.example.domain.vo.CollectionPath
import com.example.mapper.task.TaskMapper
import org.json.JSONObject
import javax.inject.Inject

/**
 * 작업(Task) 증분 동기화를 위한 SyncPort 구현체
 * MessageSyncPort와 동일한 책임 분리를 따릅니다.
 */
class TaskSyncPort @Inject constructor(
    private val taskRemoteDataSource: TaskRemoteDataSource,
    private val taskDao: TaskDao,
    private val taskMapper: TaskMapper,
    private val outboxDao: OutboxDao,
    private val channelId: String,
) : SyncPort<Task> {

    companion object {
        private const val TAG = "TaskSyncPort"
        private const val STREAM = ChannelConstants.STREAM_TASKS
    }

    override val name: String = "$STREAM-$channelId"

    // -------------------- 서버 → 로컬 (Pull) --------------------

    override suspend fun pullSince(cursor: String?, limit: Int): RemoteBatch<Task> {
        // Ensure collection path is set to this project/channel
        val path = CollectionPath.tasks(ChannelId(channelId))
        taskRemoteDataSource.setCollection(path)

        val dtoBatch = taskRemoteDataSource.pullSince(cursor, limit)
        val items = dtoBatch.items.map { dto -> taskMapper.dtoToDomain(dto) }

        return RemoteBatch(
            items = items,
            tombstones = emptyList(),
            nextCursor = dtoBatch.nextCursor,
            hasMore = dtoBatch.hasMore,
            watermark = dtoBatch.watermark
        )
    }

    override suspend fun applyRemote(
        batch: RemoteBatch<Task>,
        resolver: ConflictResolver<Task>
    ): ApplyOutcome {
        return try {
            Log.d(TAG, "applyRemote started for ${batch.items.size} tasks in channel=$channelId")
            var success = 0
            var fail = 0

            batch.items.forEach { remoteTask ->
                try {
                    Log.d(
                        TAG,
                        "Processing remote task: id=${remoteTask.id.value}, updatedAt=${remoteTask.updatedAt}"
                    )

                    // Check if local version exists and is newer (conflict detection)
                    val localTask = taskDao.findById(remoteTask.id.value)
                    if (localTask != null) {
                        val localUpdatedAt = localTask.updatedAt
                        val remoteUpdatedAt = remoteTask.updatedAt.toEpochMilli()

                        if (localUpdatedAt > remoteUpdatedAt) {
                            Log.d(
                                TAG,
                                "Skipping remote task ${remoteTask.id.value} - local is newer (local: $localUpdatedAt, remote: $remoteUpdatedAt)"
                            )
                            success++ // Count as success but don't override local
                            return@forEach
                        } else {
                            Log.d(
                                TAG,
                                "Applying remote task ${remoteTask.id.value} - remote is newer or equal (local: $localUpdatedAt, remote: $remoteUpdatedAt)"
                            )
                        }
                    } else {
                        Log.d(TAG, "Applying new remote task ${remoteTask.id.value}")
                    }

                    val entity = taskMapper.domainToEntity(remoteTask)
                    taskDao.upsert(entity)
                    Log.d(TAG, "Successfully applied remote task ${remoteTask.id.value}")
                    success++
                } catch (e: Exception) {
                    Log.e(TAG, "applyRemote failed for task=${remoteTask.id.value}", e)
                    fail++
                }
            }

            Log.d(TAG, "applyRemote completed: success=$success, fail=$fail")
            ApplyOutcome(success = success > 0)
        } catch (e: Exception) {
            Log.e(TAG, "applyRemote exception", e)
            ApplyOutcome(success = false, error = e)
        }
    }

    // -------------------- 로컬 → 서버 (Push) --------------------

    override suspend fun readOutboxBatch(limit: Int): List<OutBoxRecord> {
        // generic peek 사용 후 채널 필터링(페이로드의 channelId 기준)
        val batch = outboxDao.peek(STREAM, limit)
        return batch.mapNotNull { entity ->
            try {
                val json = JSONObject(entity.payload)
                if (json.optString(ChannelConstants.KEY_CHANNEL_ID) == channelId) entity.toModel() else null
            } catch (_: Exception) {
                null
            }
        }
    }

    override suspend fun pushToRemote(events: List<OutBoxRecord>): PushResult {
        val path = CollectionPath.tasks(ChannelId(channelId))
        taskRemoteDataSource.setCollection(path)
        return taskRemoteDataSource.push(events)
    }

    override suspend fun ackOutbox(successIds: List<String>) {
        if (successIds.isEmpty()) return
        try {
            outboxDao.markDispatched(successIds)
        } catch (e: Exception) {
            Log.e(TAG, "ackOutbox failed", e)
        }
    }

    override suspend fun retryOutBox(failed: List<FailedEvent>) {
        if (failed.isEmpty()) return
        try {
            outboxDao.markFailed(failed.map { it.id })
        } catch (e: Exception) {
            Log.e(TAG, "retryOutBox failed", e)
        }
    }
}

/**
 * 프로젝트/채널별 TaskSyncPort 팩토리
 */
class TaskSyncPortFactory @Inject constructor(
    private val taskRemoteDataSource: TaskRemoteDataSource,
    private val taskDao: TaskDao,
    private val taskMapper: TaskMapper,
    private val outboxDao: OutboxDao,
) {
    fun create(channelId: String): TaskSyncPort {
        return TaskSyncPort(
            taskRemoteDataSource,
            taskDao,
            taskMapper,
            outboxDao,
            channelId
        )
    }
}
