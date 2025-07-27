package com.example.data.repository.base

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.DMChannelRemoteDataSource
import com.example.data.datasource.remote.special.AuthRemoteDataSource
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.domain.model.base.DMChannel
import com.example.domain.repository.base.DMChannelRepository
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.factory.context.DMChannelRepositoryFactoryContext
import javax.inject.Inject

/**
 * Remote DMChannel Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
class DMChannelRepositoryImpl @Inject constructor(
    private val dmChannelRemoteDataSource: DMChannelRemoteDataSource,
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
    override val factoryContext: DMChannelRepositoryFactoryContext
) : DMChannelRepository {

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        userId: String?
    ): CustomResult<SyncResult<DMChannel>, Exception> {
        return dmChannelRemoteDataSource.syncFromServer(lastSyncCursor, userId)
    }

    override suspend fun syncToServer(
        userId: String?
    ): CustomResult<Int, Exception> {
        return dmChannelRemoteDataSource.syncToServer(userId)
    }

    override suspend fun forceSyncAll(
        userId: String?
    ): CustomResult<Int, Exception> {
        return dmChannelRemoteDataSource.forceSyncAll(userId)
    }

    override suspend fun resolveConflicts(
        conflictedChannelIds: List<String>
    ): CustomResult<Int, Exception> {
        return dmChannelRemoteDataSource.resolveConflicts(conflictedChannelIds)
    }

    // === Firebase Functions (서버 작업) ===

    override suspend fun findByOtherUserId(otherUserId: String): CustomResult<DMChannel, Exception> {
        val currentUserId = authRemoteDataSource.getCurrentUserId()
            ?: return CustomResult.Failure(Exception("User not logged in."))

        if (currentUserId == otherUserId) {
            return CustomResult.Failure(Exception("Cannot create DM channel with oneself."))
        }

        val participants = listOf(currentUserId, otherUserId)
        Log.d("DMChannelRepositoryImpl", "participants: $participants")
        val channelIdResult = dmChannelRemoteDataSource.findByParticipants(participants)
        return when (channelIdResult) {
            is CustomResult.Success -> {
                val dmChannelDTO = channelIdResult.data
                CustomResult.Success(dmChannelDTO.toDomain())
            }
            is CustomResult.Failure -> CustomResult.Failure(channelIdResult.error)
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Progress -> CustomResult.Progress(channelIdResult.progress)
        }
    }
    
    override suspend fun createDMChannel(targetUserName: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.createDMChannel(targetUserName)
    }
    
    override suspend fun blockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.blockDMChannel(channelId)
    }
    
    override suspend fun unblockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.unblockDMChannel(channelId)
    }
    
    override suspend fun unblockDMChannelByUserName(targetUserName: String): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.unblockDMChannelByUserName(targetUserName)
    }
}
