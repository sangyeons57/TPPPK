package com.example.data.repository.base

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.result.CustomResult.Initial.getOrThrow
import com.example.core_common.result.getOrNull
import com.example.core_common.result.resultTry
import com.example.core_common.util.DateTimeUtil
import com.example.data.datasource.remote.FriendRemoteDataSource
import com.example.data.datasource.remote.UserRemoteDataSource // 사용자 검색 및 정보 업데이트 시 필요
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data.model.remote.FriendDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.base.Friend
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.factory.context.FriendRepositoryFactoryContext
import com.example.domain.repository.base.FriendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 친구 관련 기능을 제공하는 Repository 구현체
 * Firebase를 사용하여 친구 관계 데이터를 관리합니다.
 */
class FriendRepositoryImpl @Inject constructor(
    private val friendRemoteDataSource: FriendRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
    override val factoryContext: FriendRepositoryFactoryContext
) : DefaultRepositoryImpl(friendRemoteDataSource, factoryContext), FriendRepository {

    private val TAG = "FriendRepository"

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Friend)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Friend"))
        ensureCollection()
        return if (entity.isNew) {
            friendRemoteDataSource.create(entity.toDto())
        } else {
            friendRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
    
    override suspend fun findFriendsByUserId(userId: String): CustomResult<List<Friend>, Exception> {
        return resultTry {
            ensureCollection(CollectionPath.users)
            friendRemoteDataSource.observeFriendsList().first().getOrThrow().map { it.toDomain() }
        }
    }
    
    override suspend fun findFriendRequestsByUserId(userId: String): CustomResult<List<Friend>, Exception> {
        return resultTry {
            ensureCollection()
            friendRemoteDataSource.observeFriendRequests().first().getOrThrow().map { it.toDomain() }
        }
    }
    
    override suspend fun findFriendByUserIdAndFriendId(userId: String, friendId: String): CustomResult<Friend?, Exception> {
        return resultTry {
            ensureCollection()
            friendRemoteDataSource.findById(DocumentId.from(friendId)).getOrNull()?.toDomain() as Friend?
        }
    }
    
    override suspend fun searchFriendsByUsername(username: String): CustomResult<List<Friend>, Exception> {
        return when (val result = friendRemoteDataSource.searchFriendsByUsername(username)){
            is CustomResult.Success -> CustomResult.Success(result.data.map { it.toDomain() })
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
    
    override fun observeFriendRequests(userId: String): Flow<CustomResult<List<Friend>, Exception>> {
        friendRemoteDataSource.setCollection(CollectionPath.userFriends(userId))
        return friendRemoteDataSource.observeFriendRequests()
            .map { result ->
                when(result) {
                    is CustomResult.Success -> CustomResult.Success(result.data.map { it.toDomain() })
                    is CustomResult.Failure -> CustomResult.Failure(result.error)
                    is CustomResult.Initial -> CustomResult.Initial
                    is CustomResult.Loading -> CustomResult.Loading
                    is CustomResult.Progress -> CustomResult.Progress(result.progress)
                }
            }
    }
    
    override fun observeFriendsList(userId: String): Flow<CustomResult<List<Friend>, Exception>> {
        friendRemoteDataSource.setCollection(CollectionPath.userFriends(userId))
        return friendRemoteDataSource.observeFriendsList()
            .map { result ->
                when(result) {
                    is CustomResult.Success -> CustomResult.Success(result.data.map { dto -> dto.toDomain()})
                    is CustomResult.Failure ->
                        CustomResult.Failure(result.error)
                    is CustomResult.Initial -> CustomResult.Initial
                    is CustomResult.Loading -> CustomResult.Loading
                    is CustomResult.Progress -> CustomResult.Progress(progress = result.progress)
                }
            }
    }
    
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
        // Note: blocking functionality may need to be implemented as a separate Firebase Function
        // For now, keeping the original implementation
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
