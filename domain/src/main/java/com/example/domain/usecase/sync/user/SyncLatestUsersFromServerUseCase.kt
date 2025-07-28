package com.example.domain.usecase.sync.user

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.local.LocalUserRepository
import com.example.domain.repository.remote.DefaultRepository
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.entity.SyncMetadataEntity
import com.example.data_core.entity.SyncDirection
import com.google.firebase.firestore.Query
import java.time.Instant
import javax.inject.Inject

/**
 * 서버에서 최신 사용자 데이터를 동기화하는 UseCase
 * SyncMetadata cursor를 기반으로 최신 데이터부터 가져옵니다.
 */
class SyncLatestUsersFromServerUseCase @Inject constructor(
    private val remoteRepository: DefaultRepository<User>,
    private val localRepository: LocalUserRepository,
    private val syncMetadataDao: SyncMetadataDao
) {
    
    suspend operator fun invoke(limit: Int = 50): CustomResult<SyncResult, Exception> {
        return try {
            // 1. SyncMetadata에서 마지막 동기화 위치 확인
            val collectionName = "users"
            val syncMetadata = syncMetadataDao.getSyncMetadata(collectionName, SyncDirection.DOWN)
                ?: SyncMetadataEntity(
                    collectionName = collectionName,
                    lastServerCursor = null,
                    lastLocalCursor = null,
                    lastSyncTimestamp = Instant.now(),
                    syncDirection = SyncDirection.DOWN
                )
            
            // 2. 서버에서 데이터 조회 (최신 순)
            remoteRepository.setCollection(CollectionPath.from(collectionName))
            val serverResult = remoteRepository.findNByUpdatedAt(
                n = limit.toLong(),
                updatedAt = syncMetadata.lastSyncTimestamp,
                direction = Query.Direction.DESCENDING
            )
            
            val users = when (serverResult) {
                is CustomResult.Success -> serverResult.data
                is CustomResult.Failure -> return CustomResult.Failure(serverResult.error)
                else -> return CustomResult.Failure(Exception("Unexpected result state"))
            }
            
            // 3. 로컬에 저장
            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<Exception>()
            
            users.forEach { user ->
                when (val saveResult = localRepository.saveEntity(user)) {
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
            
            // 4. SyncMetadata 업데이트
            val updatedMetadata = syncMetadata.copy(
                lastSyncTimestamp = Instant.now(),
                lastServerCursor = users.lastOrNull()?.id?.value
            )
            syncMetadataDao.upsertSyncMetadata(updatedMetadata)
            
            CustomResult.Success(
                SyncResult(
                    totalCount = users.size,
                    successCount = successCount,
                    errorCount = errorCount,
                    errors = errors
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
        val errors: List<Exception>
    )
}