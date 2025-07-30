package com.example.data_repository.local

import com.example.core_common.result.CustomResult
import com.example.data_datasource.local.MessageDataSource
import com.example.domain.model.base.Message
import com.example.domain.model.enum.SyncStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain_repository.local.LocalMessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Message 로컬 저장소 Repository 구현체
 * MessageDataSource와 OutBoxDataSource를 주입받아 Message 도메인 특화 로컬 저장소 구현
 * 모든 변경 작업은 OutBox에 동기화 작업 추가
 *
 * TODO: This class needs to be refactored to match the new repository pattern.
 * Temporarily disabled to allow mapper refactoring to proceed.
 */
@Singleton
class LocalMessageRepositoryImpl @Inject constructor(
    private val messageDataSource: MessageDataSource
) : BaseLocalRepositoryImpl<Message>(), LocalMessageRepository {

    // ================================
    // BaseLocalRepositoryImpl 추상 메서드 구현
    // ================================

    override suspend fun saveToDataSource(domain: Message): CustomResult<Unit, Exception> {
        return try {
            messageDataSource.save(domain)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun saveAllToDataSource(entities: List<Message>): CustomResult<Unit, Exception> {
        return try {
            messageDataSource.saveAll(entities)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun deleteFromDataSource(entity: Message): CustomResult<Unit, Exception> {
        return try {
            messageDataSource.delete(entity)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun findByIdFromDataSource(id: DocumentId): CustomResult<Message?, Exception> {
        return messageDataSource.getById(id.value)
    }
    
    override suspend fun findAllFromDataSource(): CustomResult<List<Message>, Exception> {
        return messageDataSource.getAll()
    }
    
    override fun observeFromDataSource(id: DocumentId): Flow<CustomResult<Message?, Exception>> {
        return messageDataSource.observeById(id.value)
    }
    
    override fun observeAllFromDataSource(): Flow<CustomResult<List<Message>, Exception>> {
        return messageDataSource.observeAll()
    }
    
    override suspend fun deleteByIdFromDataSource(id: DocumentId): CustomResult<Int, Exception> {
        return try {
            messageDataSource.deleteById(id.value)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun deleteMarkedEntitiesFromDataSource(): CustomResult<Int, Exception> {
        return messageDataSource.deleteMarkedMessages()
    }
    
    override suspend fun getTotalCountFromDataSource(): CustomResult<Int, Exception> {
        return messageDataSource.getTotalCount()
    }
    
    override suspend fun existsInDataSource(id: DocumentId): CustomResult<Boolean, Exception> {
        return messageDataSource.exists(id.value)
    }
    
    override suspend fun getAllForDebugFromDataSource(): CustomResult<List<Message>, Exception> {
        return messageDataSource.getAllForDebug()
    }
    
    override suspend fun deleteAllFromDataSource(): CustomResult<Unit, Exception> {
        return messageDataSource.deleteAll()
    }

    // ================================
    // Message 도메인 특화 조회 작업 구현
    // ================================
    
    override suspend fun getMessagesAfter(
        afterTimestamp: Long, 
        limit: Int
    ): CustomResult<List<Message>, Exception> {
        return try {
            messageDataSource.getMessagesAfter(afterTimestamp, limit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun getMessagesBefore(
        beforeTimestamp: Long, 
        limit: Int
    ): CustomResult<List<Message>, Exception> {
        return try {
            messageDataSource.getMessagesBefore(beforeTimestamp, limit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun getMessagesBetween(
        startTimestamp: Long, 
        endTimestamp: Long
    ): CustomResult<List<Message>, Exception> {
        return try {
            messageDataSource.getMessagesBetween(startTimestamp, endTimestamp)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 사용자 기반 조회 작업 구현
    // ================================
    
    override suspend fun getMessagesBySender(senderId: UserId): CustomResult<List<Message>, Exception> {
        return try {
            messageDataSource.getMessagesBySender(senderId.value)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun getRepliesByMessageId(replyToMessageId: DocumentId): CustomResult<List<Message>, Exception> {
        return try {
            messageDataSource.getRepliesByMessageId(replyToMessageId.value)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 동기화 관련 작업 구현
    // ================================
    
    override suspend fun getMessagesBySyncStatus(syncStatus: SyncStatus): CustomResult<List<Message>, Exception> {
        return try {
            messageDataSource.getMessagesBySyncStatus(syncStatus)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun getUnsyncedMessages(): CustomResult<List<Message>, Exception> {
        return try {
            messageDataSource.getUnsyncedMessages()
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun getErrorMessages(): CustomResult<List<Message>, Exception> {
        return try {
            messageDataSource.getErrorMessages()
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun getMessagesAfterVersion(version: Long): CustomResult<List<Message>, Exception> {
        return try {
            messageDataSource.getMessagesAfterVersion(version)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun updateSyncStatus(
        messageId: DocumentId,
        syncStatus: SyncStatus,
        serverVersion: Long?,
        serverUpdatedAt: Long?
    ): CustomResult<Unit, Exception> {
        return try {
            messageDataSource.updateSyncStatus(
                messageId.value,
                syncStatus,
                serverVersion,
                serverUpdatedAt
            )
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 배치 처리 작업 구현
    // ================================
    
    override suspend fun getUnsyncedMessagesWithLimit(limit: Int): CustomResult<List<Message>, Exception> {
        return try {
            messageDataSource.getUnsyncedMessagesWithLimit(limit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun updateSyncStatusByIds(
        messageIds: List<DocumentId>, 
        newSyncStatus: SyncStatus
    ): CustomResult<Int, Exception> {
        return try {
            val stringIds = messageIds.map { it.value }
            messageDataSource.updateSyncStatusByIds(stringIds, newSyncStatus)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 정리 및 관리 작업 구현
    // ================================
    
    override suspend fun deleteOldMessages(beforeTimestamp: Long): CustomResult<Int, Exception> {
        return try {
            messageDataSource.deleteOldMessages(beforeTimestamp)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 통계 및 개수 조회 구현
    // ================================
    
    override suspend fun getUnsyncedCount(): CustomResult<Int, Exception> {
        return try {
            messageDataSource.getUnsyncedCount()
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    override suspend fun getSyncStatusStatistics(): CustomResult<Map<SyncStatus, Int>, Exception> {
        return try {
            messageDataSource.getSyncStatusStatistics()
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}