package com.example.data_repository.base

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

    override suspend fun addTask(payload: Task) {
        // 1) 로컬 Room에 선저장 (SSOT 기반)
        val local = TaskEntity(
            id = payload.id.value,
            channelId = payload.channelId.value,
            taskType = payload.taskType.value,
            status = payload.status.value,
            content = payload.content.value,
            order = payload.order.value,
            checkedBy = payload.checkedBy?.value,
            checkedAt = payload.checkedAt?.toEpochMilli(),
            createdAt = payload.createdAt.toEpochMilli(),
            updatedAt = payload.updatedAt.toEpochMilli(),
        )
        taskDao.upsert(local)

        // 2) Outbox에 enqueue하여 비동기 서버 동기화 위임
        val outBoxRecord = OutBoxRecord(
            id = UUID.randomUUID().toString(),
            stream = Task.COLLECTION_NAME,
            aggregateId = local.id,
            op = OutBoxRecord.Op.UPSERT,
            payload = OutboxPayloadUtil.toPayload(local),
            createdAt = System.currentTimeMillis(),
        )

        outboxDao.enqueue(outBoxRecord.toEntity(OutBoxStatus.PENDING))
    }

    override fun observeByProject(projectId: String): Flow<CustomResult<List<Task>, Exception>> {
        return taskDao.observeByProject(projectId).map { entities ->
            CustomResult.Success(entities.map { taskFullMapper.entityToDomain(it) })
        }
    }

    override fun observeByChannel(channelId: String): Flow<CustomResult<List<Task>, Exception>> {
        return taskDao.observeByChannel(channelId).map { entities ->
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

    override suspend fun delete(id: DocumentId): CustomResult<Unit, Exception> {
        return try {
            // 1) Local delete
            taskDao.deleteById(id.value)

            // 2) Enqueue Outbox DELETE
            val now = System.currentTimeMillis()
            val payload = """{"id":"${id.value}","deletedAt":$now}"""
            val record = OutBoxRecord(
                id = UUID.randomUUID().toString(),
                stream = Task.COLLECTION_NAME,
                aggregateId = id.value,
                op = OutBoxRecord.Op.DELETE,
                payload = payload,
                createdAt = now
            )
            outboxDao.enqueue(record.toEntity(OutBoxStatus.PENDING))

            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    override suspend fun save(entity: Task): CustomResult<DocumentId, Exception> {
        val errorMessage = """
            ❌ TaskRepository에서 save() 사용 금지!

            태스크 저장은 SSOT(로컬 우선 + Outbox) 방식을 사용하세요:
            - addTask(): 로컬 업서트 + Outbox enqueue

            올바른 사용법:
            taskRepository.addTask(task)
        """.trimIndent()

        android.util.Log.e("TaskRepository", errorMessage)
        return CustomResult.Failure(
            UnsupportedOperationException(
                "Use addTask() (SSOT+Outbox) instead of save()"
            )
        )
    }
}
