package com.example.domain.usecase.sync.schedule

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Schedule
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.LocalScheduleRepository
import com.example.domain.repository.remote.DefaultRepository
import com.example.data_core.dao.OutboxDao
import com.example.data_core.entity.OutboxEntity
import javax.inject.Inject

/**
 * 로컬에서 대기 중인 모든 일정 변경사항을 서버로 동기화하는 UseCase
 * Outbox 패턴을 사용하여 오프라인 상태에서 발생한 변경사항을 처리합니다.
 */
class SyncPendingScheduleChangesToServerUseCase @Inject constructor(
    private val remoteRepository: DefaultRepository<Schedule>,
    private val localRepository: LocalScheduleRepository,
    private val outboxDao: OutboxDao
) {
    
    suspend operator fun invoke(): CustomResult<SyncResult, Exception> {
        return try {
            // 1. 일정 관련 대기 중인 Outbox 항목들 조회
            val collectionName = "schedules"
            val pendingEntries = outboxDao.getPendingEntriesByCollection(collectionName)
            
            if (pendingEntries.isEmpty()) {
                return CustomResult.Success(
                    SyncResult(
                        totalCount = 0,
                        successCount = 0,
                        errorCount = 0,
                        errors = emptyList()
                    )
                )
            }
            
            // 2. 서버 Repository 설정
            remoteRepository.setCollection(CollectionPath.from(collectionName))
            
            // 3. 각 항목을 서버에 동기화
            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<Exception>()
            val processedEntries = mutableListOf<OutboxEntity>()
            
            pendingEntries.forEach { outboxEntry ->
                try {
                    val syncResult = when (outboxEntry.operation) {
                        "CREATE" -> handleCreate(outboxEntry)
                        "UPDATE" -> handleUpdate(outboxEntry)
                        "DELETE" -> handleDelete(outboxEntry)
                        else -> CustomResult.Failure(Exception("Unknown operation: ${outboxEntry.operation}"))
                    }
                    
                    when (syncResult) {
                        is CustomResult.Success -> {
                            successCount++
                            processedEntries.add(outboxEntry)
                        }
                        is CustomResult.Failure -> {
                            errorCount++
                            errors.add(syncResult.error)
                        }
                        else -> {
                            errorCount++
                            errors.add(Exception("Unexpected sync result state"))
                        }
                    }
                } catch (exception: Exception) {
                    errorCount++
                    errors.add(exception)
                }
            }
            
            // 4. 성공적으로 처리된 Outbox 항목들 삭제
            processedEntries.forEach { entry ->
                outboxDao.deleteOutboxEntry(entry.id)
            }
            
            CustomResult.Success(
                SyncResult(
                    totalCount = pendingEntries.size,
                    successCount = successCount,
                    errorCount = errorCount,
                    errors = errors
                )
            )
            
        } catch (exception: Exception) {
            CustomResult.Failure(exception)
        }
    }
    
    private suspend fun handleCreate(outboxEntry: OutboxEntity): CustomResult<Unit, Exception> {
        // 로컬에서 일정 조회 후 서버에 생성
        val scheduleResult = localRepository.getEntityById(outboxEntry.documentId)
        return when (scheduleResult) {
            is CustomResult.Success -> {
                val schedule = scheduleResult.data
                remoteRepository.create(schedule).let { result ->
                    when (result) {
                        is CustomResult.Success -> CustomResult.Success(Unit)
                        is CustomResult.Failure -> CustomResult.Failure(result.error)
                        else -> CustomResult.Failure(Exception("Unexpected create result state"))
                    }
                }
            }
            is CustomResult.Failure -> CustomResult.Failure(scheduleResult.error)
            else -> CustomResult.Failure(Exception("Schedule not found locally for CREATE operation"))
        }
    }
    
    private suspend fun handleUpdate(outboxEntry: OutboxEntity): CustomResult<Unit, Exception> {
        // 로컬에서 일정 조회 후 서버에 업데이트
        val scheduleResult = localRepository.getEntityById(outboxEntry.documentId)
        return when (scheduleResult) {
            is CustomResult.Success -> {
                val schedule = scheduleResult.data
                val updateData = mapOf(
                    "title" to schedule.title,
                    "description" to schedule.description,
                    "startDateTime" to schedule.startDateTime,
                    "endDateTime" to schedule.endDateTime,
                    "isAllDay" to schedule.isAllDay,
                    "location" to schedule.location,
                    "status" to schedule.status.name,
                    "projectId" to schedule.projectId?.value,
                    "createdBy" to schedule.createdBy.value,
                    "updatedAt" to schedule.updatedAt
                )
                
                remoteRepository.update(DocumentId.from(outboxEntry.documentId), updateData).let { result ->
                    when (result) {
                        is CustomResult.Success -> CustomResult.Success(Unit)
                        is CustomResult.Failure -> CustomResult.Failure(result.error)
                        else -> CustomResult.Failure(Exception("Unexpected update result state"))
                    }
                }
            }
            is CustomResult.Failure -> CustomResult.Failure(scheduleResult.error)
            else -> CustomResult.Failure(Exception("Schedule not found locally for UPDATE operation"))
        }
    }
    
    private suspend fun handleDelete(outboxEntry: OutboxEntity): CustomResult<Unit, Exception> {
        // 서버에서 일정 삭제
        return remoteRepository.delete(DocumentId.from(outboxEntry.documentId)).let { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> CustomResult.Failure(Exception("Unexpected delete result state"))
            }
        }
    }
    
    data class SyncResult(
        val totalCount: Int,
        val successCount: Int,
        val errorCount: Int,
        val errors: List<Exception>
    )
}