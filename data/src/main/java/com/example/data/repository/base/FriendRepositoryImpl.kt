package com.example.data.repository.base

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.FriendRemoteDataSource
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.domain.model.base.Friend
import com.example.domain.repository.base.FriendRepository
import com.example.domain.repository.base.SyncResult
import com.example.domain.repository.factory.context.FriendRepositoryFactoryContext
import javax.inject.Inject

/**
 * Remote Friend Repository Implementation (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 */
class FriendRepositoryImpl @Inject constructor(
    private val friendRemoteDataSource: FriendRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
    override val factoryContext: FriendRepositoryFactoryContext
) : FriendRepository {

    private val TAG = "FriendRepository"

    override suspend fun syncFromServer(
        lastSyncCursor: Long?,
        userId: String?
    ): CustomResult<SyncResult<Friend>, Exception> {
        return friendRemoteDataSource.syncFromServer(lastSyncCursor, userId)
    }

    override suspend fun syncToServer(
        userId: String?
    ): CustomResult<Int, Exception> {
        return friendRemoteDataSource.syncToServer(userId)
    }

    override suspend fun forceSyncAll(
        userId: String?
    ): CustomResult<Int, Exception> {
        return friendRemoteDataSource.forceSyncAll(userId)
    }

    override suspend fun resolveConflicts(
        conflictedFriendIds: List<String>
    ): CustomResult<Int, Exception> {
        return friendRemoteDataSource.resolveConflicts(conflictedFriendIds)
    }

    // === Firebase Functions (서버 작업) ===
    
    override suspend fun sendFriendRequest(fromUserId: String, toUserId: String): CustomResult<Unit, Exception> {
        Log.d(TAG, "sendFriendRequest called: fromUserId=$fromUserId, toUserId=$toUserId")
        return when (val result = functionsRemoteDataSource.sendFriendRequest(toUserId)) {
            is CustomResult.Success -> {
                Log.d(TAG, "Friend request sent successfully: ${result.data}")
                CustomResult.Success(Unit)
            }
            is CustomResult.Failure -> {
                Log.e(TAG, "Failed to send friend request", result.error)
                CustomResult.Failure(result.error)
            }
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
    
    override suspend fun acceptFriendRequest(userId: String, friendId: String): CustomResult<Unit, Exception> {
        return when (val result = functionsRemoteDataSource.acceptFriendRequest(friendId)) {
            is CustomResult.Success -> {
                Log.d(TAG, "Friend request accepted successfully: ${result.data}")
                CustomResult.Success(Unit)
            }
            is CustomResult.Failure -> {
                Log.e(TAG, "Failed to accept friend request", result.error)
                CustomResult.Failure(result.error)
            }
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
    
    override suspend fun declineFriendRequest(userId: String, friendId: String): CustomResult<Unit, Exception> {
        return when (val result = functionsRemoteDataSource.rejectFriendRequest(friendId)) {
            is CustomResult.Success -> {
                Log.d(TAG, "Friend request declined successfully: ${result.data}")
                CustomResult.Success(Unit)
            }
            is CustomResult.Failure -> {
                Log.e(TAG, "Failed to decline friend request", result.error)
                CustomResult.Failure(result.error)
            }
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
    
    override suspend fun blockUser(userId: String, friendId: String): CustomResult<Unit, Exception> {
        return friendRemoteDataSource.blockUser(userId, friendId)
    }
    
    override suspend fun removeFriend(userId: String, friendId: String): CustomResult<Unit, Exception> {
        return when (val result = functionsRemoteDataSource.removeFriend(friendId)) {
            is CustomResult.Success -> {
                Log.d(TAG, "Friend removed successfully: ${result.data}")
                CustomResult.Success(Unit)
            }
            is CustomResult.Failure -> {
                Log.e(TAG, "Failed to remove friend", result.error)
                CustomResult.Failure(result.error)
            }
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}
