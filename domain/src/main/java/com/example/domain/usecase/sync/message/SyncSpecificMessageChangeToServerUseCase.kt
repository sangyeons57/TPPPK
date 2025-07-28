package com.example.domain.usecase.sync.message

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.LocalMessageRepository
import com.example.domain.repository.remote.DefaultRepository
import com.example.data_core.dao.OutboxDao
import com.example.data_core.entity.OutboxEntity
import javax.inject.Inject

/**
 * 로컬에서 특정 메시지 변경사항을 서버로 동기화하는 UseCase
 * 특정 Outbox 항목 하나를 즉시 서버에 동기화합니다.
 */
class SyncSpecificMessageChangeToServerUseCase @Inject constructor(
    private val remoteRepository: DefaultRepository<Message>,
    private val localRepository: LocalMessageRepository,
    private val outboxDao: OutboxDao
) {
    
    suspend operator fun invoke(outboxId: String): CustomResult<SyncResult, Exception> {
        return try {
            // 1. 특정 Outbox 항목 조회
            val outboxEntry = outboxDao.getOutboxEntryById(outboxId)
                ?: return CustomResult.Failure(Exception("Outbox entry not found: $outboxId"))
            
            // 2. 메시지 컬렉션인지 확인
            if (outboxEntry.collectionName != "messages") {
                return CustomResult.Failure(Exception("Invalid collection name for message sync: ${outboxEntry.collectionName}"))
            }
            
            // 3. 서버 Repository 설정
            remoteRepository.setCollection(CollectionPath.from("messages"))
            
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
        // 로컬에서 메시지 조회 후 서버에 생성
        val messageResult = localRepository.getEntityById(outboxEntry.documentId)
        return when (messageResult) {
            is CustomResult.Success -> {
                val message = messageResult.data
                remoteRepository.create(message).let { result ->
                    when (result) {
                        is CustomResult.Success -> CustomResult.Success(Unit)
                        is CustomResult.Failure -> CustomResult.Failure(result.error)
                        else -> CustomResult.Failure(Exception("Unexpected create result state"))
                    }
                }
            }
            is CustomResult.Failure -> CustomResult.Failure(messageResult.error)
            else -> CustomResult.Failure(Exception("Message not found locally for CREATE operation"))
        }
    }
    
    private suspend fun handleUpdate(outboxEntry: OutboxEntity): CustomResult<Unit, Exception> {
        // 로컬에서 메시지 조회 후 서버에 업데이트
        val messageResult = localRepository.getEntityById(outboxEntry.documentId)
        return when (messageResult) {
            is CustomResult.Success -> {
                val message = messageResult.data
                val updateData = mapOf(
                    "content" to message.content.value,
                    "editedAt" to message.editedAt,
                    "isDeleted" to message.isDeleted,
                    "authorId" to message.authorId.value,
                    "channelId" to message.channelId?.value,
                    "projectId" to message.projectId?.value,
                    "parentMessageId" to message.parentMessageId?.value,
                    "updatedAt" to message.updatedAt
                )
                
                remoteRepository.update(DocumentId.from(outboxEntry.documentId), updateData).let { result ->
                    when (result) {
                        is CustomResult.Success -> CustomResult.Success(Unit)
                        is CustomResult.Failure -> CustomResult.Failure(result.error)
                        else -> CustomResult.Failure(Exception("Unexpected update result state"))
                    }
                }
            }
            is CustomResult.Failure -> CustomResult.Failure(messageResult.error)
            else -> CustomResult.Failure(Exception("Message not found locally for UPDATE operation"))
        }
    }
    
    private suspend fun handleDelete(outboxEntry: OutboxEntity): CustomResult<Unit, Exception> {
        // 서버에서 메시지 삭제
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