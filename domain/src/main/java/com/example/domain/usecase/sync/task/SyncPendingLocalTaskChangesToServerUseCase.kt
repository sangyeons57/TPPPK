package com.example.domain.usecase.sync.task

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.LocalTaskRepository
import com.example.domain.repository.remote.DefaultRepository
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.entity.SyncMetadataEntity
import com.example.data_core.entity.SyncDirection
import com.google.firebase.firestore.Query
import java.time.Instant
import javax.inject.Inject

/**
 * 로컬에서 변경된 태스크 데이터를 서버로 동기화하는 UseCase
 * SyncMetadata를 사용하여 마지막 동기화 이후 변경된 로컬 데이터만 처리합니다.
 */
class SyncPendingLocalTaskChangesToServerUseCase @Inject constructor(
    private val remoteRepository: DefaultRepository<Task>,
    private val localRepository: LocalTaskRepository,
    private val syncMetadataDao: SyncMetadataDao
) {
    
    suspend operator fun invoke(): CustomResult<SyncResult, Exception> {
        return try {
            // 1. SyncMetadata에서 마지막 동기화 시간 확인
            val collectionName = "tasks"
            val syncMetadata = syncMetadataDao.getSyncMetadata(collectionName, SyncDirection.UP)
                ?: SyncMetadataEntity(
                    collectionName = collectionName,
                    lastServerCursor = null,
                    lastLocalCursor = null,
                    lastSyncTimestamp = Instant.now().minusSeconds(3600), // 1시간 전부터
                    syncDirection = SyncDirection.UP
                )
            
            // 2. 로컬에서 변경된 태스크 데이터 조회 (마지막 동기화 이후)
            val localResult = localRepository.findUpdatedAfter(syncMetadata.lastSyncTimestamp)
            val tasks = when (localResult) {
                is CustomResult.Success -> localResult.data
                is CustomResult.Failure -> return CustomResult.Failure(localResult.error)
                else -> return CustomResult.Failure(Exception("Unexpected result state"))
            }
            
            // 변경사항이 없으면 조기 반환
            if (tasks.isEmpty()) {
                return CustomResult.Success(
                    SyncResult(
                        totalCount = 0,
                        successCount = 0,
                        errorCount = 0,
                        errors = emptyList(),
                        isUpSync = true
                    )
                )
            }
            
            // 3. 서버에 업로드
            remoteRepository.setCollection(CollectionPath.from(collectionName))
            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<Exception>()
            
            tasks.forEach { task ->
                try {
                    // 서버에 이미 존재하는지 확인 후 CREATE 또는 UPDATE 결정
                    val remoteResult = remoteRepository.findById(task.id)
                    val syncResult = when (remoteResult) {
                        is CustomResult.Success -> {
                            // 존재하면 업데이트
                            val updateData = mapOf(
                                "title" to task.title,
                                "description" to task.description,
                                "status" to task.status.name,
                                "priority" to task.priority.name,
                                "dueDate" to task.dueDate,
                                "isCompleted" to task.isCompleted,
                                "completedAt" to task.completedAt,
                                "assigneeId" to task.assigneeId?.value,
                                "projectId" to task.projectId.value,
                                "createdBy" to task.createdBy.value,
                                "order" to task.order,
                                "updatedAt" to task.updatedAt
                            )
                            remoteRepository.update(task.id, updateData)
                        }
                        is CustomResult.Failure -> {
                            // 존재하지 않으면 생성
                            remoteRepository.create(task)
                        }
                        else -> CustomResult.Failure(Exception("Unexpected remote check result"))
                    }
                    
                    when (syncResult) {
                        is CustomResult.Success -> successCount++
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
            
            // 4. SyncMetadata 업데이트 (가장 최근 업데이트 시간으로)
            val latestUpdateTime = tasks.maxByOrNull { it.updatedAt }?.updatedAt ?: Instant.now()
            val updatedMetadata = syncMetadata.copy(
                lastSyncTimestamp = latestUpdateTime,
                lastLocalCursor = tasks.firstOrNull()?.id?.value
            )
            syncMetadataDao.upsertSyncMetadata(updatedMetadata)
            
            CustomResult.Success(
                SyncResult(
                    totalCount = tasks.size,
                    successCount = successCount,
                    errorCount = errorCount,
                    errors = errors,
                    isUpSync = true
                )
            )
            
        } catch (exception: Exception) {
            CustomResult.Failure(exception)
        }
    }
    
    data class SyncResult(
        val totalCount: Int,
        val successCount: Int,
        val errorCount: Int,
        val errors: List<Exception>,
        val isUpSync: Boolean = false
    )
}