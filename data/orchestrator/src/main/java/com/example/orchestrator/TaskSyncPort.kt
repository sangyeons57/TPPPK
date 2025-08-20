package com.example.orchestrator

import android.util.Log
import com.example.data_datasource.remote.TaskRemoteDataSource
import com.example.core_common.constants.ChannelConstants
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
import com.example.domain.vo.CollectionPath
import com.example.mapper.task.TaskMapper
import com.example.domain.vo.ChannelId
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
            var success = 0
            var fail = 0
            batch.items.forEach { task ->
                try {
                    val entity = taskMapper.domainToEntity(task)
                    taskDao.upsert(entity)
                    success++
                } catch (e: Exception) {
                    Log.e(TAG, "applyRemote failed for task=${task.id.value}", e)
                    fail++
                }
            }
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
