package com.example.domain.usecase.sync.message

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.local.LocalMessageRepository
import com.example.domain.repository.remote.DefaultRepository
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.entity.SyncMetadataEntity
import com.example.data_core.entity.SyncDirection
import com.google.firebase.firestore.Query
import java.time.Instant
import javax.inject.Inject

/**
 * 서버에서 증분 메시지 데이터를 동기화하는 UseCase
 * 마지막 동기화 시간 이후 변경된 데이터만 가져옵니다.
 */
class SyncIncrementalMessagesFromServerUseCase @Inject constructor(
    private val remoteRepository: DefaultRepository<Message>,
    private val localRepository: LocalMessageRepository,
    private val syncMetadataDao: SyncMetadataDao
) {
    
    suspend operator fun invoke(): CustomResult<SyncResult, Exception> {
        return try {
            // 1. SyncMetadata에서 마지막 동기화 시간 확인
            val collectionName = "messages"
            val syncMetadata = syncMetadataDao.getSyncMetadata(collectionName, SyncDirection.DOWN)
                ?: SyncMetadataEntity(
                    collectionName = collectionName,
                    lastServerCursor = null,
                    lastLocalCursor = null,
                    lastSyncTimestamp = Instant.now().minusSeconds(3600), // 1시간 전부터
                    syncDirection = SyncDirection.DOWN
                )
            
            // 2. 서버에서 증분 데이터 조회 (마지막 동기화 이후)
            remoteRepository.setCollection(CollectionPath.from(collectionName))
            val serverResult = remoteRepository.findNByUpdatedAt(
                n = 1000L, // 증분 동기화는 큰 제한으로 설정
                updatedAt = syncMetadata.lastSyncTimestamp,
                direction = Query.Direction.DESCENDING
            )
            
            val messages = when (serverResult) {
                is CustomResult.Success -> serverResult.data.filter { 
                    it.updatedAt.isAfter(syncMetadata.lastSyncTimestamp) 
                }
                is CustomResult.Failure -> return CustomResult.Failure(serverResult.error)
                else -> return CustomResult.Failure(Exception("Unexpected result state"))
            }
            
            // 변경사항이 없으면 조기 반환
            if (messages.isEmpty()) {
                return CustomResult.Success(
                    SyncResult(
                        totalCount = 0,
                        successCount = 0,
                        errorCount = 0,
                        errors = emptyList(),
                        isIncremental = true
                    )
                )
            }
            
            // 3. 로컬에 저장
            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<Exception>()
            
            messages.forEach { message ->
                when (val saveResult = localRepository.saveEntity(message)) {
                    is CustomResult.Success -> successCount++
                    is CustomResult.Failure -> {
                        errorCount++
                        errors.add(saveResult.error)
                    }
                    else -> {
                        errorCount++
                        errors.add(Exception("Unexpected save result state"))
                    }
                }
            }
            
            // 4. SyncMetadata 업데이트 (가장 최근 업데이트 시간으로)
            val latestUpdateTime = messages.maxByOrNull { it.updatedAt }?.updatedAt ?: Instant.now()
            val updatedMetadata = syncMetadata.copy(
                lastSyncTimestamp = latestUpdateTime,
                lastServerCursor = messages.firstOrNull()?.id?.value
            )
            syncMetadataDao.upsertSyncMetadata(updatedMetadata)
            
            CustomResult.Success(
                SyncResult(
                    totalCount = messages.size,
                    successCount = successCount,
                    errorCount = errorCount,
                    errors = errors,
                    isIncremental = true
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
        val isIncremental: Boolean = false
    )
}