package com.example.data_repository.base

import androidx.room.Transaction
import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.TaskRemoteDataSource
import com.example.data_model.local.OutboxDao
import com.example.data_model.local.TaskDao
import com.example.data_model.local.TaskEntity
import com.example.data_model.local.toEntity
import com.example.data_model.remote.TaskDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.data_repository.util.OutboxPayloadUtil
import com.example.domain.enum.OutBoxStatus
import com.example.domain.model.base.Task
import com.example.domain.model.sync.OutBoxRecord
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.TaskRepository
import com.example.mapper.DtoMapper
import com.example.mapper.Mapper
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskRemoteDataSource: TaskRemoteDataSource,
    private val taskMapper: DtoMapper<Task, TaskDTO>,
    private val outboxDao: OutboxDao,
    private val taskDao: TaskDao,
    private val taskFullMapper: Mapper<TaskEntity, Task, TaskDTO>,
) : DefaultRepositoryImpl<Task, TaskDTO>(taskRemoteDataSource, taskMapper), TaskRepository {

    @Transaction
    override suspend fun createTask(payload: Task) {
        try {
            android.util.Log.d(
                "TaskRepository",
                "[CREATE] Starting createTask for NEW taskId=${payload.id.value}, channelId=${payload.channelId.value}"
            )

            // 1) NEW TASK 검증: 기존 태스크가 있다면 오류
            val existing = taskDao.findById(payload.id.value)
            if (existing != null) {
                throw IllegalStateException("Task already exists: ${payload.id.value}. Use updateTask() instead.")
            }

            // 2) 로컬 Room에 INSERT (SSOT 기반) - CREATE 전용
            val local = TaskEntity(
                id = payload.id.value,
                channelId = payload.channelId.value,
                taskType = payload.taskType.value,
                status = payload.status.value,
                content = payload.content.value,
                order = payload.order.value,
                checkedBy = payload.checkedBy?.value,
                checkedAt = payload.checkedAt?.toEpochMilli(),
                deletedAt = null,
                createdAt = payload.createdAt.toEpochMilli(),
                updatedAt = payload.updatedAt.toEpochMilli(),
            )

            taskDao.upsert(local)
            android.util.Log.d(
                "TaskRepository",
                "[CREATE] Room insert completed for NEW taskId=${payload.id.value}"
            )

            // 3) Outbox에 CREATE 전용 enqueue (서버에 새 태스크 생성 요청)
            val outBoxRecord = OutBoxRecord(
                id = UUID.randomUUID().toString(),
                stream = Task.COLLECTION_NAME,
                aggregateId = local.id,
                op = OutBoxRecord.Op.UPSERT, // 서버에서는 새 문서 생성
                payload = OutboxPayloadUtil.toPayload(local),
                createdAt = System.currentTimeMillis(),
            )

            outboxDao.enqueue(outBoxRecord.toEntity(OutBoxStatus.PENDING))
            android.util.Log.d(
                "TaskRepository",
                "[CREATE] Outbox CREATE enqueue completed for taskId=${payload.id.value}"
            )

        } catch (e: Exception) {
            android.util.Log.e(
                "TaskRepository",
                "[CREATE] Failed to createTask for taskId=${payload.id.value}",
                e
            )
            throw e
        }
    }

    @Transaction
    override suspend fun updateTask(payload: Task) {
        try {
            android.util.Log.d(
                "TaskRepository",
                "[UPDATE] Starting updateTask for EXISTING taskId=${payload.id.value}"
            )

            // 1) EXISTING TASK 검증: 기존 태스크가 없다면 오류
            val existing = taskDao.findById(payload.id.value)
            if (existing == null) {
                throw IllegalStateException("Task not found: ${payload.id.value}. Use createTask() instead.")
            }

            // 2) 변경사항 감지 (성능 최적화)
            val hasChanges = existing.content != payload.content.value ||
                    existing.taskType != payload.taskType.value ||
                    existing.status != payload.status.value ||
                    existing.order != payload.order.value ||
                    existing.checkedBy != payload.checkedBy?.value

            if (!hasChanges) {
                android.util.Log.d(
                    "TaskRepository",
                    "[UPDATE] No changes detected for taskId=${payload.id.value}, skipping update"
                )
                return
            }

            // 3) 로컬 Room에 UPDATE (SSOT 기반) - UPDATE 전용
            val local = TaskEntity(
                id = payload.id.value,
                channelId = payload.channelId.value,
                taskType = payload.taskType.value,
                status = payload.status.value,
                content = payload.content.value,
                order = payload.order.value,
                checkedBy = payload.checkedBy?.value,
                checkedAt = payload.checkedAt?.toEpochMilli(),
                deletedAt = null,
                createdAt = existing.createdAt, // 기존 생성시간 유지
                updatedAt = payload.updatedAt.toEpochMilli(), // 수정시간만 업데이트
            )

            taskDao.upsert(local)
            android.util.Log.d(
                "TaskRepository",
                "[UPDATE] Room update completed for taskId=${payload.id.value}"
            )

            // 4) Outbox에 UPDATE 전용 enqueue (서버에 기존 문서 수정 요청)
            val outBoxRecord = OutBoxRecord(
                id = UUID.randomUUID().toString(),
                stream = Task.COLLECTION_NAME,
                aggregateId = local.id,
                op = OutBoxRecord.Op.UPSERT, // 서버에서는 기존 문서 업데이트
                payload = OutboxPayloadUtil.toPayload(local),
                createdAt = System.currentTimeMillis(),
            )

            outboxDao.enqueue(outBoxRecord.toEntity(OutBoxStatus.PENDING))
            android.util.Log.d(
                "TaskRepository",
                "[UPDATE] Outbox UPDATE enqueue completed for taskId=${payload.id.value}"
            )

        } catch (e: Exception) {
            android.util.Log.e(
                "TaskRepository",
                "[UPDATE] Failed to updateTask for taskId=${payload.id.value}",
                e
            )
            throw e
        }
    }

    override fun observeByProject(projectId: String): Flow<CustomResult<List<Task>, Exception>> {
        android.util.Log.d("TaskRepository", "Starting observeByProject for projectId=$projectId")
        return taskDao.observeByProject(projectId).map { entities ->
            android.util.Log.d(
                "TaskRepository",
                "observeByProject emitted ${entities.size} tasks for projectId=$projectId"
            )
            CustomResult.Success(entities.map { taskFullMapper.entityToDomain(it) })
        }
    }

    override fun observeByChannel(channelId: String): Flow<CustomResult<List<Task>, Exception>> {
        android.util.Log.d("TaskRepository", "Starting observeByChannel for channelId=$channelId")
        return taskDao.observeByChannel(channelId).map { entities ->
            android.util.Log.d(
                "TaskRepository",
                "observeByChannel emitted ${entities.size} tasks for channelId=$channelId"
            )
            entities.forEach { entity ->
                android.util.Log.d(
                    "TaskRepository",
                    "Task: id=${entity.id}, content=${entity.content}, order=${entity.order}"
                )
            }
            CustomResult.Success(entities.map { taskFullMapper.entityToDomain(it) })
        }
    }

    override suspend fun findById(id: DocumentId, source: Source): CustomResult<Task, Exception> {
        return try {
            val entity = taskDao.findById(id.value)
            if (entity != null) {
                CustomResult.Success(taskFullMapper.entityToDomain(entity))
            } else {
                CustomResult.Failure(IllegalStateException("Task not found locally: ${id.value}"))
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    @Transaction
    override suspend fun delete(id: DocumentId): CustomResult<Unit, Exception> {
        return try {
            android.util.Log.d("TaskRepository", "Starting delete for taskId=${id.value}")

            // 1) Read entity first to capture channelId for OutBox payload filtering
            val existing = taskDao.findById(id.value)

            // 2) Local soft delete (tombstone)
            val now = System.currentTimeMillis()
            if (existing != null) {
                val tombstoned = existing.copy(
                    deletedAt = now,
                    updatedAt = now
                )
                taskDao.upsert(tombstoned)
                android.util.Log.d(
                    "TaskRepository",
                    "Room soft delete (tombstone) applied for taskId=${id.value}"
                )
            }

            // 3) Enqueue Outbox DELETE
            // Include channelId so TaskSyncPort can filter this event for the correct channel
            val payload = buildString {
                append('{')
                existing?.let { entity ->
                    append("\"channelId\":\"")
                    append(entity.channelId)
                    append("\",")
                }
                append("\"id\":\"")
                append(id.value)
                append("\",")
                append("\"deletedAt\":")
                append(now)
                append('}')
            }
            val record = OutBoxRecord(
                id = UUID.randomUUID().toString(),
                stream = Task.COLLECTION_NAME,
                aggregateId = id.value,
                op = OutBoxRecord.Op.DELETE,
                payload = payload,
                createdAt = now
            )
            outboxDao.enqueue(record.toEntity(OutBoxStatus.PENDING))
            android.util.Log.d(
                "TaskRepository",
                "Outbox delete enqueue completed for taskId=${id.value}"
            )

            CustomResult.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to delete taskId=${id.value}", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun save(entity: Task): CustomResult<DocumentId, Exception> {
        val errorMessage = """
            ❌ TaskRepository에서 save() 사용 금지!

            태스크 저장은 SSOT(로컬 우선 + Outbox) 방식을 사용하세요:
            - createTask(): 새로운 태스크 생성 + Outbox enqueue
            - updateTask(): 기존 태스크 업데이트 + Outbox enqueue

            올바른 사용법:
            taskRepository.createTask(task)  // 새 태스크
            taskRepository.updateTask(task)  // 기존 태스크 수정
        """.trimIndent()

        android.util.Log.e("TaskRepository", errorMessage)
        return CustomResult.Failure(
            UnsupportedOperationException(
                "Use createTask() or updateTask() (SSOT+Outbox) instead of save()"
            )
        )
    }
}
