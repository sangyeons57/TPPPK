package com.example.data_datasource.local

import com.example.core_common.result.CustomResult
import com.example.data_datasource.dao.MessageDao
import com.example.domain.model.base.Message
import com.example.domain.model.enum.SyncStatus
import com.example.mapper.message.MessageMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Message 로컬 데이터 소스 구현체
 * MessageDao를 래핑하고 Entity ↔ Domain Model 변환 처리
 */
@Singleton
class MessageDataSourceImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val messageMapper: MessageMapper
) : MessageDataSource {

    // ================================
    // Repository용 - 기본 CRUD 작업
    // ================================
    
    override suspend fun save(message: Message): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = messageMapper.domainToEntity(message)
                messageDao.insert(entity)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun saveAll(messages: List<Message>): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = messages.map { messageMapper.domainToEntity(it) }
                messageDao.insertAll(entities)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun delete(message: Message): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                // 메시지를 삭제된 상태로 업데이트
                message.delete() // Domain method to mark as deleted
                val entity = messageMapper.domainToEntity(message)
                messageDao.update(entity)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // Repository용 - 기본 조회 작업
    // ================================
    
    override suspend fun getById(id: String): CustomResult<Message?, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = messageDao.getById(id)
                val domainModel = entity?.let { messageMapper.entityToDomain(it) }
                CustomResult.Success(domainModel)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getAll(): CustomResult<List<Message>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = messageDao.getAll()
                val domainModels = entities.map { messageMapper.entityToDomain(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override fun observeById(id: String): Flow<CustomResult<Message?, Exception>> {
        return messageDao.observeById(id)
            .map { entity ->
                try {
                    val domainModel = entity?.let { messageMapper.entityToDomain(it) }
                    CustomResult.Success(domainModel)
                } catch (e: Exception) {
                    CustomResult.Failure(e)
                }
            }
            .catch { e ->
                emit(CustomResult.Failure(e as? Exception ?: Exception(e.message)))
            }
            .flowOn(Dispatchers.IO)
    }
    
    override fun observeAll(): Flow<CustomResult<List<Message>, Exception>> {
        return messageDao.observeAll()
            .map { entities ->
                try {
                    val domainModels = entities.map { messageMapper.entityToDomain(it) }
                    CustomResult.Success(domainModels)
                } catch (e: Exception) {
                    CustomResult.Failure(e)
                }
            }
            .catch { e ->
                emit(CustomResult.Failure(e as? Exception ?: Exception(e.message)))
            }
            .flowOn(Dispatchers.IO)
    }

    // ================================
    // Repository용 - 시간 범위별 조회
    // ================================
    
    override suspend fun getMessagesAfter(
        afterTimestamp: Long, 
        limit: Int
    ): CustomResult<List<Message>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = messageDao.getMessagesAfter(afterTimestamp, limit)
                val domainModels = entities.map { messageMapper.entityToDomain(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getMessagesBefore(
        beforeTimestamp: Long, 
        limit: Int
    ): CustomResult<List<Message>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = messageDao.getMessagesBefore(beforeTimestamp, limit)
                val domainModels = entities.map { messageMapper.entityToDomain(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getMessagesBetween(
        startTimestamp: Long, 
        endTimestamp: Long
    ): CustomResult<List<Message>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = messageDao.getMessagesBetween(startTimestamp, endTimestamp)
                val domainModels = entities.map { messageMapper.entityToDomain(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // Repository용 - 사용자별 조회
    // ================================
    
    override suspend fun getMessagesBySender(senderId: String): CustomResult<List<Message>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = messageDao.getMessagesBySender(senderId)
                val domainModels = entities.map { messageMapper.entityToDomain(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getRepliesByMessageId(replyToMessageId: String): CustomResult<List<Message>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = messageDao.getRepliesByMessageId(replyToMessageId)
                val domainModels = entities.map { messageMapper.entityToDomain(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 동기화 관련 작업
    // ================================
    
    override suspend fun getMessagesBySyncStatus(syncStatus: SyncStatus): CustomResult<List<Message>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = messageDao.getMessagesBySyncStatus(syncStatus.name)
                val domainModels = entities.map { messageMapper.entityToDomain(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getUnsyncedMessages(): CustomResult<List<Message>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = messageDao.getUnsyncedMessages()
                val domainModels = entities.map { messageMapper.entityToDomain(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getErrorMessages(): CustomResult<List<Message>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = messageDao.getErrorMessages()
                val domainModels = entities.map { messageMapper.entityToDomain(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getMessagesAfterVersion(version: Long): CustomResult<List<Message>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = messageDao.getMessagesAfterVersion(version)
                val domainModels = entities.map { messageMapper.entityToDomain(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun updateSyncStatus(
        messageId: String,
        syncStatus: SyncStatus,
        serverVersion: Long?,
        serverUpdatedAt: Long?
    ): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                // 기존 엔티티를 가져와서 동기화 상태만 업데이트
                val existingEntity = messageDao.getById(messageId)
                    ?: return@withContext CustomResult.Failure(Exception("Message not found: $messageId"))
                
                val updatedEntity = existingEntity.copy(
                    syncStatus = syncStatus.name,
                    serverVersion = serverVersion ?: existingEntity.serverVersion,
                    serverUpdatedAt = serverUpdatedAt ?: existingEntity.serverUpdatedAt
                )
                
                messageDao.update(updatedEntity)
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // SyncManager용 - 배치 처리
    // ================================
    
    override suspend fun getUnsyncedMessagesWithLimit(limit: Int): CustomResult<List<Message>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = messageDao.getUnsyncedMessages().take(limit)
                val domainModels = entities.map { messageMapper.entityToDomain(it) }
                CustomResult.Success(domainModels)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun updateSyncStatusByIds(
        messageIds: List<String>, 
        newSyncStatus: SyncStatus
    ): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                var updatedCount = 0
                messageIds.forEach { messageId ->
                    val existingEntity = messageDao.getById(messageId)
                    if (existingEntity != null) {
                        val updatedEntity = existingEntity.copy(syncStatus = newSyncStatus.name)
                        messageDao.update(updatedEntity)
                        updatedCount++
                    }
                }
                CustomResult.Success(updatedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // Repository용 - 정리 및 관리
    // ================================
    
    override suspend fun deleteById(id: String): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val deletedCount = messageDao.deleteById(id)
                CustomResult.Success(deletedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun deleteOldMessages(beforeTimestamp: Long): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val deletedCount = messageDao.deleteOldMessages(beforeTimestamp)
                CustomResult.Success(deletedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun deleteMarkedMessages(): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val deletedCount = messageDao.deleteMarkedMessages()
                CustomResult.Success(deletedCount)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // Repository용 - 통계 및 개수 조회
    // ================================
    
    override suspend fun getTotalCount(): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val count = messageDao.getTotalCount()
                CustomResult.Success(count)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getUnsyncedCount(): CustomResult<Int, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val count = messageDao.getUnsyncedCount()
                CustomResult.Success(count)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun getSyncStatusStatistics(): CustomResult<Map<SyncStatus, Int>, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val statistics = mutableMapOf<SyncStatus, Int>()
                
                SyncStatus.values().forEach { status ->
                    val entities = messageDao.getMessagesBySyncStatus(status.name)
                    statistics[status] = entities.size
                }
                
                CustomResult.Success(statistics.toMap())
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // Repository용 - 존재 여부 확인
    // ================================
    
    override suspend fun exists(id: String): CustomResult<Boolean, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                val exists = messageDao.exists(id)
                CustomResult.Success(exists)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    // ================================
    // 테스트 및 디버깅용
    // ================================
    
    override suspend fun getAllForDebug(): CustomResult<List<Message>, Exception> {
        return getAll() // 동일한 구현
    }
    
    override suspend fun deleteAll(): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                messageDao.deleteAll()
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
}