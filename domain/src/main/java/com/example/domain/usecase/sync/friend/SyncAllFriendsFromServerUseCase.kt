package com.example.domain.usecase.sync.friend

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Friend
import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.local.LocalFriendRepository
import com.example.domain.repository.remote.DefaultRepository
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.entity.SyncMetadataEntity
import com.example.data_core.entity.SyncDirection
import java.time.Instant
import javax.inject.Inject

/**
 * 서버에서 모든 친구 데이터를 강제로 동기화하는 UseCase
 * 로컬 데이터를 완전히 초기화하고 서버 데이터로 교체합니다.
 */
class SyncAllFriendsFromServerUseCase @Inject constructor(
    private val remoteRepository: DefaultRepository<Friend>,
    private val localRepository: LocalFriendRepository,
    private val syncMetadataDao: SyncMetadataDao
) {
    
    suspend operator fun invoke(): CustomResult<SyncResult, Exception> {
        return try {
            // 1. 서버에서 모든 친구 데이터 조회
            val collectionName = "friends"
            remoteRepository.setCollection(CollectionPath.from(collectionName))
            val serverResult = remoteRepository.findAll()
            
            val friends = when (serverResult) {
                is CustomResult.Success -> serverResult.data
                is CustomResult.Failure -> return CustomResult.Failure(serverResult.error)
                else -> return CustomResult.Failure(Exception("Unexpected result state"))
            }
            
            // 2. 로컬 데이터 전체 삭제
            when (val clearResult = localRepository.deleteAllEntities()) {
                is CustomResult.Success -> { /* 성공 */ }
                is CustomResult.Failure -> return CustomResult.Failure(clearResult.error)
                else -> return CustomResult.Failure(Exception("Failed to clear local data"))
            }
            
            // 3. 서버 데이터를 로컬에 저장
            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<Exception>()
            
            friends.forEach { friend ->
                when (val saveResult = localRepository.saveEntity(friend)) {
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
            
            // 4. SyncMetadata 업데이트 (완전 동기화 완료 상태로)
            val currentTime = Instant.now()
            val syncMetadata = SyncMetadataEntity(
                collectionName = collectionName,
                lastServerCursor = friends.firstOrNull()?.id?.value,
                lastLocalCursor = null,
                lastSyncTimestamp = currentTime,
                syncDirection = SyncDirection.DOWN
            )
            syncMetadataDao.upsertSyncMetadata(syncMetadata)
            
            CustomResult.Success(
                SyncResult(
                    totalCount = friends.size,
                    successCount = successCount,
                    errorCount = errorCount,
                    errors = errors,
                    isFullSync = true
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
        val isFullSync: Boolean = false
    )
}