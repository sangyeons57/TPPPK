package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.LocalFriendsDataSource
import com.example.data_core.repository.local.base.BaseLocalRepositoryImpl
import com.example.domain.model.base.Friend
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.local.LocalFriendRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Friend Repository Implementation (SSOT)
 * BaseLocalRepositoryImpl 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - BaseLocalRepositoryImpl의 공통 CRUD 기능 상속 (80%)
 * - Friend 도메인 특화 기능만 구현 (20%)
 * - LocalDataSource를 통한 Room DB 접궼
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 메서드 구현:
 * - observeEntityById -> observeFriendById로 위임
 * - observeAllEntities -> observeAllFriends로 위임
 * - observeEntityUpdatedAt -> observeFriendUpdatedAt로 위임
 * - getEntityById -> getFriendById로 위임
 * - getEntitiesByIds -> getFriendsByIds로 위임
 * - getAllEntities -> getAllFriends로 위임
 * - saveEntity -> saveFriend로 위임
 * - saveEntities -> saveFriends로 위임
 * - deleteEntity -> deleteFriend로 위임
 * - Plus SyncableRepository methods
 */
@Singleton
class LocalFriendRepositoryImpl @Inject constructor(
    private val localFriendsDataSource: LocalFriendsDataSource
) : BaseLocalRepositoryImpl<Friend>(), LocalFriendRepository {

    companion object {
        private const val TAG = "LocalFriendRepository"
    }

    // === BaseLocalRepository 메서드 구현 (도메인 특화 메서드로 위임) ===

    override fun observeEntityById(entityId: String): Flow<Friend?> = 
        observeFriendById(entityId)

    override fun observeAllEntities(): Flow<List<Friend>> = 
        observeAllFriends()

    override fun observeEntityUpdatedAt(entityId: String): Flow<Long?> = 
        observeFriendUpdatedAt(entityId)

    override suspend fun getEntityById(entityId: String): CustomResult<Friend?, Exception> = 
        handleOperation("getFriendById($entityId)", TAG) {
            getFriendById(entityId)
        }

    override suspend fun getEntitiesByIds(entityIds: List<String>): CustomResult<List<Friend>, Exception> = 
        handleOperation("getFriendsByIds(${entityIds.size})", TAG) {
            getFriendsByIds(entityIds)
        }

    override suspend fun getAllEntities(limit: Int?): CustomResult<List<Friend>, Exception> = 
        handleOperation("getAllFriends($limit)", TAG) {
            getAllFriends(limit)
        }

    override suspend fun saveEntity(entity: Friend): CustomResult<Unit, Exception> = 
        saveFriend(entity)

    override suspend fun saveEntities(entities: List<Friend>): CustomResult<Unit, Exception> = 
        saveFriends(entities)

    override suspend fun deleteEntity(entityId: String): CustomResult<Unit, Exception> = 
        deleteFriend(entityId)

    override suspend fun getEntitiesUpdatedAfter(timestamp: Instant): CustomResult<List<Friend>, Exception> = 
        handleOperation("getFriendsUpdatedAfter($timestamp)", TAG) {
            getFriendsUpdatedAfter(timestamp)
        }

    override suspend fun clearAllEntities(): CustomResult<Unit, Exception> = 
        clearAllFriends()

    override suspend fun getTotalEntityCount(): CustomResult<Int, Exception> = 
        handleOperation("getTotalFriendCount", TAG) {
            getTotalFriendCount()
        }

    override suspend fun entityExists(entityId: String): CustomResult<Boolean, Exception> = 
        handleOperation("friendExists($entityId)", TAG) {
            friendExists(entityId)
        }

    override suspend fun addToOutbox(
        entityId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return handleOperation("addToOutbox($entityId, $operation)", TAG) {
            localFriendsDataSource.addToOutbox(entityId, operation, payload)
        }
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeFriendById(friendId: String): Flow<Friend?> {
        logDebug( "observeFriendById: $friendId")
        return localFriendsDataSource.observeFriendById(friendId)
    }

    override fun observeFriendByName(name: UserName): Flow<Friend?> {
        logDebug( "observeFriendByName: ${name.value}")
        return localFriendsDataSource.observeFriendByName(name.value)
    }

    override fun observeFriendsByName(name: String, limit: Int): Flow<List<Friend>> {
        logDebug( "observeFriendsByName: name='$name', limit=$limit")
        return localFriendsDataSource.observeFriendsByName(name, limit)
    }

    override fun observeFriendsByStatus(status: FriendStatus): Flow<List<Friend>> {
        logDebug( "observeFriendsByStatus: $status")
        return localFriendsDataSource.observeFriendsByStatus(status)
    }

    override fun observeAcceptedFriends(): Flow<List<Friend>> {
        logDebug( "observeAcceptedFriends")
        return observeFriendsByStatus(FriendStatus.ACCEPTED)
    }

    override fun observeSentFriendRequests(): Flow<List<Friend>> {
        logDebug( "observeSentFriendRequests")
        return observeFriendsByStatus(FriendStatus.REQUESTED)
    }

    override fun observeReceivedFriendRequests(): Flow<List<Friend>> {
        logDebug( "observeReceivedFriendRequests")
        return observeFriendsByStatus(FriendStatus.PENDING)
    }

    override fun observeBlockedFriends(): Flow<List<Friend>> {
        logDebug( "observeBlockedFriends")
        return observeFriendsByStatus(FriendStatus.BLOCKED)
    }

    override fun observeAllFriends(): Flow<List<Friend>> {
        logDebug( "observeAllFriends")
        return localFriendsDataSource.observeAllFriends()
    }

    override fun observeFriendUpdatedAt(friendId: String): Flow<Long?> {
        logDebug( "observeFriendUpdatedAt: $friendId")
        return kotlinx.coroutines.flow.map(observeFriendById(friendId)) { friend ->
            friend?.updatedAt?.toEpochMilli()
        }
    }

    override fun observeFriends(friendIds: List<String>): Flow<List<Friend>> {
        logDebug( "observeFriends: ${friendIds.size} friends")
        return localFriendsDataSource.observeFriends(friendIds)
    }

    // === 단순 읽기 작업 ===

    override suspend fun getFriendById(friendId: String): Friend? {
        logDebug( "getFriendById: $friendId")
        return try {
            localFriendsDataSource.getFriendById(friendId)
        } catch (e: Exception) {
            logError( "getFriendById failed", e)
            null
        }
    }

    override suspend fun getFriendByName(name: UserName): Friend? {
        logDebug( "getFriendByName: ${name.value}")
        return try {
            localFriendsDataSource.getFriendByName(name.value)
        } catch (e: Exception) {
            logError( "getFriendByName failed", e)
            null
        }
    }

    override suspend fun searchFriendsByName(name: String, limit: Int): List<Friend> {
        logDebug( "searchFriendsByName: name='$name', limit=$limit")
        return try {
            localFriendsDataSource.searchFriendsByName(name, limit)
        } catch (e: Exception) {
            logError( "searchFriendsByName failed", e)
            emptyList()
        }
    }

    override suspend fun getFriendsByStatus(status: FriendStatus): List<Friend> {
        logDebug( "getFriendsByStatus: $status")
        return try {
            localFriendsDataSource.getFriendsByStatus(status)
        } catch (e: Exception) {
            logError( "getFriendsByStatus failed", e)
            emptyList()
        }
    }

    override suspend fun getAcceptedFriends(): List<Friend> {
        logDebug( "getAcceptedFriends")
        return getFriendsByStatus(FriendStatus.ACCEPTED)
    }

    override suspend fun getSentFriendRequests(): List<Friend> {
        logDebug( "getSentFriendRequests")
        return getFriendsByStatus(FriendStatus.REQUESTED)
    }

    override suspend fun getReceivedFriendRequests(): List<Friend> {
        logDebug( "getReceivedFriendRequests")
        return getFriendsByStatus(FriendStatus.PENDING)
    }

    override suspend fun getBlockedFriends(): List<Friend> {
        logDebug( "getBlockedFriends")
        return getFriendsByStatus(FriendStatus.BLOCKED)
    }

    override suspend fun getAllFriends(limit: Int?): List<Friend> {
        logDebug( "getAllFriends: limit=$limit")
        return try {
            localFriendsDataSource.getAllFriends(limit)
        } catch (e: Exception) {
            logError( "getAllFriends failed", e)
            emptyList()
        }
    }

    override suspend fun getFriendsByIds(friendIds: List<String>): List<Friend> {
        logDebug( "getFriendsByIds: ${friendIds.size} friends")
        return try {
            localFriendsDataSource.getFriendsByIds(friendIds)
        } catch (e: Exception) {
            logError( "getFriendsByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getFriendsRequestedAfter(timestamp: Instant): List<Friend> {
        logDebug( "getFriendsRequestedAfter: $timestamp")
        return try {
            getAllFriends().filter { friend ->
                friend.requestedAt?.isAfter(timestamp) == true
            }
        } catch (e: Exception) {
            logError( "getFriendsRequestedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun getFriendsAcceptedAfter(timestamp: Instant): List<Friend> {
        logDebug( "getFriendsAcceptedAfter: $timestamp")
        return try {
            getAllFriends().filter { friend ->
                friend.acceptedAt?.isAfter(timestamp) == true
            }
        } catch (e: Exception) {
            logError( "getFriendsAcceptedAfter failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveFriend(friend: Friend): CustomResult<Unit, Exception> {
        return try {
            logDebug( "saveFriend: ${friend.id}")

            // 1. Room DB에 저장
            localFriendsDataSource.saveFriend(friend)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (friend.isNew) "CREATE" else "UPDATE"
            localFriendsDataSource.addToOutbox(
                friendId = friend.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            logDebug( "Friend saved and added to outbox: ${friend.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError( "saveFriend failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveFriends(friends: List<Friend>): CustomResult<Unit, Exception> {
        return try {
            logDebug( "saveFriends: ${friends.size} friends")

            if (friends.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localFriendsDataSource.saveFriends(friends)

            logDebug( "Bulk friends saved: ${friends.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError( "saveFriends failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteFriend(friendId: String): CustomResult<Unit, Exception> {
        return try {
            logDebug( "deleteFriend: $friendId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localFriendsDataSource.deleteFriend(friendId)

            // 2. Outbox에 삭제 작업 추가
            localFriendsDataSource.addToOutbox(
                friendId = friendId,
                operation = "DELETE",
                payload = null
            )

            logDebug( "Friend deleted and added to outbox: $friendId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError( "deleteFriend failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateFriend(
        friendId: String,
        name: UserName?,
        profileImageUrl: ImageUrl?,
        status: FriendStatus?
    ): CustomResult<Unit, Exception> {
        return try {
            logDebug( "updateFriend: friendId=$friendId")

            // 1. 현재 친구 조회
            val currentFriend = localFriendsDataSource.getFriendById(friendId)
                ?: return CustomResult.Failure(IllegalArgumentException("Friend not found: $friendId"))

            // 2. 업데이트 적용
            var updatedFriend = currentFriend

            name?.let { updatedFriend.changeName(it) }
            profileImageUrl?.let { updatedFriend.changeProfileImage(it) }

            when (status) {
                FriendStatus.ACCEPTED -> updatedFriend.acceptRequest()
                FriendStatus.BLOCKED -> updatedFriend.blockUser()
                FriendStatus.REMOVED -> updatedFriend.removeFriend()
                FriendStatus.PENDING -> updatedFriend.markAsPending()
                FriendStatus.REQUESTED -> updatedFriend.markAsRequested()
                null -> { /* No status change */
                }
            }

            // 3. 저장 (Outbox 포함)
            saveFriend(updatedFriend)

            logDebug( "Friend updated: $friendId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError( "updateFriend failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun acceptFriendRequest(friendId: String): CustomResult<Unit, Exception> {
        return updateFriend(friendId, status = FriendStatus.ACCEPTED)
    }

    override suspend fun blockFriend(friendId: String): CustomResult<Unit, Exception> {
        return updateFriend(friendId, status = FriendStatus.BLOCKED)
    }

    override suspend fun removeFriend(friendId: String): CustomResult<Unit, Exception> {
        return updateFriend(friendId, status = FriendStatus.REMOVED)
    }

    override suspend fun markFriendRequestAsPending(friendId: String): CustomResult<Unit, Exception> {
        return updateFriend(friendId, status = FriendStatus.PENDING)
    }

    override suspend fun markFriendRequestAsRequested(friendId: String): CustomResult<Unit, Exception> {
        return updateFriend(friendId, status = FriendStatus.REQUESTED)
    }

    override suspend fun changeFriendName(
        friendId: String,
        newName: UserName
    ): CustomResult<Unit, Exception> {
        return updateFriend(friendId, name = newName)
    }

    override suspend fun changeFriendProfileImage(
        friendId: String,
        newProfileImageUrl: ImageUrl?
    ): CustomResult<Unit, Exception> {
        return updateFriend(friendId, profileImageUrl = newProfileImageUrl)
    }

    // === 유틸리티 ===

    override suspend fun friendExists(friendId: String): Boolean {
        return try {
            localFriendsDataSource.friendExists(friendId)
        } catch (e: Exception) {
            logError( "friendExists failed", e)
            false
        }
    }

    override suspend fun nameExists(name: UserName, excludeFriendId: String?): Boolean {
        return try {
            localFriendsDataSource.nameExists(name.value, excludeFriendId)
        } catch (e: Exception) {
            logError( "nameExists failed", e)
            false
        }
    }

    override suspend fun getFriendCountByStatus(status: FriendStatus): Int {
        return try {
            localFriendsDataSource.getFriendCountByStatus(status)
        } catch (e: Exception) {
            logError( "getFriendCountByStatus failed", e)
            0
        }
    }

    override suspend fun getTotalFriendCount(): Int {
        return try {
            localFriendsDataSource.getTotalFriendCount()
        } catch (e: Exception) {
            logError( "getTotalFriendCount failed", e)
            0
        }
    }

    override suspend fun getAcceptedFriendCount(): Int {
        return getFriendCountByStatus(FriendStatus.ACCEPTED)
    }

    override suspend fun getSentFriendRequestCount(): Int {
        return getFriendCountByStatus(FriendStatus.REQUESTED)
    }

    override suspend fun getReceivedFriendRequestCount(): Int {
        return getFriendCountByStatus(FriendStatus.PENDING)
    }

    override suspend fun getBlockedFriendCount(): Int {
        return getFriendCountByStatus(FriendStatus.BLOCKED)
    }

    override suspend fun clearAllFriends(): CustomResult<Unit, Exception> {
        return try {
            logDebug( "clearAllFriends")

            localFriendsDataSource.clearAllFriends()

            logDebug( "All friends cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError( "clearAllFriends failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getFriendsUpdatedAfter(timestamp: Instant): List<Friend> {
        return try {
            localFriendsDataSource.getFriendsUpdatedAfter(timestamp)
        } catch (e: Exception) {
            logError( "getFriendsUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        friendId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            logDebug( "addToOutbox: friendId=$friendId, operation=$operation")

            localFriendsDataSource.addToOutbox(friendId, operation, payload)

            logDebug( "Added to outbox: $friendId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            logError( "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}