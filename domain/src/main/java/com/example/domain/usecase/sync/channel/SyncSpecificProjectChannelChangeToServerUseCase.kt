package com.example.domain.usecase.sync.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.LocalProjectChannelRepository
import com.example.domain.repository.remote.DefaultRepository
import com.example.data_core.dao.OutboxDao
import com.example.data_core.entity.OutboxEntity
import javax.inject.Inject

/**
 * 로컬에서 특정 프로젝트 채널 변경사항을 서버로 동기화하는 UseCase
 * 특정 Outbox 항목 하나를 즉시 서버에 동기화합니다.
 */
class SyncSpecificProjectChannelChangeToServerUseCase @Inject constructor(
    private val remoteRepository: DefaultRepository<ProjectChannel>,
    private val localRepository: LocalProjectChannelRepository,
    private val outboxDao: OutboxDao
) {
    
    suspend operator fun invoke(outboxId: String): CustomResult<SyncResult, Exception> {
        return try {
            // 1. 특정 Outbox 항목 조회
            val outboxEntry = outboxDao.getOutboxEntryById(outboxId)
                ?: return CustomResult.Failure(Exception("Outbox entry not found: $outboxId"))
            
            // 2. 프로젝트 채널 컬렉션인지 확인
            if (outboxEntry.collectionName != "projectChannels") {
                return CustomResult.Failure(Exception("Invalid collection name for project channel sync: ${outboxEntry.collectionName}"))
            }
            
            // 3. 서버 Repository 설정
            remoteRepository.setCollection(CollectionPath.from("projectChannels"))
            
            // 4. 작업 타입에 따라 동기화 수행
            val syncResult = when (outboxEntry.operation) {
                "CREATE" -> handleCreate(outboxEntry)
                "UPDATE" -> handleUpdate(outboxEntry)
                "DELETE" -> handleDelete(outboxEntry)
                else -> CustomResult.Failure(Exception("Unknown operation: ${outboxEntry.operation}"))
            }
            
            // 5. 성공 시 Outbox 항목 삭제
            when (syncResult) {
                is CustomResult.Success -> {
                    outboxDao.deleteOutboxEntry(outboxId)
                    CustomResult.Success(
                        SyncResult(
                            outboxId = outboxId,
                            operation = outboxEntry.operation,
                            documentId = outboxEntry.documentId,
                            success = true,
                            error = null
                        )
                    )
                }
                is CustomResult.Failure -> {
                    CustomResult.Success(
                        SyncResult(
                            outboxId = outboxId,
                            operation = outboxEntry.operation,
                            documentId = outboxEntry.documentId,
                            success = false,
                            error = syncResult.error
                        )
                    )
                }
                else -> {
                    CustomResult.Failure(Exception("Unexpected sync result state"))
                }
            }
            
        } catch (exception: Exception) {
            CustomResult.Failure(exception)
        }
    }
    
    private suspend fun handleCreate(outboxEntry: OutboxEntity): CustomResult<Unit, Exception> {
        // 로컬에서 프로젝트 채널 조회 후 서버에 생성
        val channelResult = localRepository.getEntityById(outboxEntry.documentId)
        return when (channelResult) {
            is CustomResult.Success -> {
                val channel = channelResult.data
                remoteRepository.create(channel).let { result ->
                    when (result) {
                        is CustomResult.Success -> CustomResult.Success(Unit)
                        is CustomResult.Failure -> CustomResult.Failure(result.error)
                        else -> CustomResult.Failure(Exception("Unexpected create result state"))
                    }
                }
            }
            is CustomResult.Failure -> CustomResult.Failure(channelResult.error)
            else -> CustomResult.Failure(Exception("ProjectChannel not found locally for CREATE operation"))
        }
    }
    
    private suspend fun handleUpdate(outboxEntry: OutboxEntity): CustomResult<Unit, Exception> {
        // 로컬에서 프로젝트 채널 조회 후 서버에 업데이트
        val channelResult = localRepository.getEntityById(outboxEntry.documentId)
        return when (channelResult) {
            is CustomResult.Success -> {
                val channel = channelResult.data
                val updateData = mapOf(
                    "name" to channel.name.value,
                    "description" to channel.description?.value,
                    "position" to channel.position?.value,
                    "categoryId" to channel.categoryId?.value,
                    "projectId" to channel.projectId?.value,
                    "updatedAt" to channel.updatedAt
                )
                
                remoteRepository.update(DocumentId.from(outboxEntry.documentId), updateData).let { result ->
                    when (result) {
                        is CustomResult.Success -> CustomResult.Success(Unit)
                        is CustomResult.Failure -> CustomResult.Failure(result.error)
                        else -> CustomResult.Failure(Exception("Unexpected update result state"))
                    }
                }
            }
            is CustomResult.Failure -> CustomResult.Failure(channelResult.error)
            else -> CustomResult.Failure(Exception("ProjectChannel not found locally for UPDATE operation"))
        }
    }
    
    private suspend fun handleDelete(outboxEntry: OutboxEntity): CustomResult<Unit, Exception> {
        // 서버에서 프로젝트 채널 삭제
        return remoteRepository.delete(DocumentId.from(outboxEntry.documentId)).let { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(Unit)
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                else -> CustomResult.Failure(Exception("Unexpected delete result state"))
            }
        }
    }
    
    data class SyncResult(
        val outboxId: String,
        val operation: String,
        val documentId: String,
        val success: Boolean,
        val error: Exception?
    )
}