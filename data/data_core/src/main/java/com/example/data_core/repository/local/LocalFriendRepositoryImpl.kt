package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.local.LocalFriendsDataSource
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
 * Room Database 전용 구현체 - UI에 직접 데이터 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - LocalDataSource를 통한 Room DB 접근
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 */
@Singleton
class LocalFriendRepositoryImpl @Inject constructor(
    private val localFriendsDataSource: LocalFriendsDataSource
) : LocalFriendRepository {

    companion object {
        private const val TAG = "LocalFriendRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeFriendById(friendId: String): Flow<Friend?> {
        Log.d(TAG, "observeFriendById: $friendId")
        return localFriendsDataSource.observeFriendById(friendId)
    }

    override fun observeFriendByName(name: UserName): Flow<Friend?> {
        Log.d(TAG, "observeFriendByName: ${name.value}")
        return localFriendsDataSource.observeFriendByName(name.value)
    }

    override fun observeFriendsByName(name: String, limit: Int): Flow<List<Friend>> {
        Log.d(TAG, "observeFriendsByName: name='$name', limit=$limit")
        return localFriendsDataSource.observeFriendsByName(name, limit)
    }

    override fun observeFriendsByStatus(status: FriendStatus): Flow<List<Friend>> {
        Log.d(TAG, "observeFriendsByStatus: $status")
        return localFriendsDataSource.observeFriendsByStatus(status)
    }

    override fun observeAcceptedFriends(): Flow<List<Friend>> {
        Log.d(TAG, "observeAcceptedFriends")
        return observeFriendsByStatus(FriendStatus.ACCEPTED)
    }

    override fun observeSentFriendRequests(): Flow<List<Friend>> {
        Log.d(TAG, "observeSentFriendRequests")
        return observeFriendsByStatus(FriendStatus.REQUESTED)
    }

    override fun observeReceivedFriendRequests(): Flow<List<Friend>> {
        Log.d(TAG, "observeReceivedFriendRequests")
        return observeFriendsByStatus(FriendStatus.PENDING)
    }

    override fun observeBlockedFriends(): Flow<List<Friend>> {
        Log.d(TAG, "observeBlockedFriends")
        return observeFriendsByStatus(FriendStatus.BLOCKED)
    }

    override fun observeAllFriends(): Flow<List<Friend>> {
        Log.d(TAG, "observeAllFriends")
        return localFriendsDataSource.observeAllFriends()
    }

    override fun observeFriendUpdatedAt(friendId: String): Flow<Long?> {
        Log.d(TAG, "observeFriendUpdatedAt: $friendId")
        return kotlinx.coroutines.flow.map(observeFriendById(friendId)) { friend ->
            friend?.updatedAt?.toEpochMilli()
        }
    }

    override fun observeFriends(friendIds: List<String>): Flow<List<Friend>> {
        Log.d(TAG, "observeFriends: ${friendIds.size} friends")
        return localFriendsDataSource.observeFriends(friendIds)
    }

    // === 단순 읽기 작업 ===

    override suspend fun getFriendById(friendId: String): Friend? {
        Log.d(TAG, "getFriendById: $friendId")
        return try {
            localFriendsDataSource.getFriendById(friendId)
        } catch (e: Exception) {
            Log.e(TAG, "getFriendById failed", e)
            null
        }
    }

    override suspend fun getFriendByName(name: UserName): Friend? {
        Log.d(TAG, "getFriendByName: ${name.value}")
        return try {
            localFriendsDataSource.getFriendByName(name.value)
        } catch (e: Exception) {
            Log.e(TAG, "getFriendByName failed", e)
            null
        }
    }

    override suspend fun searchFriendsByName(name: String, limit: Int): List<Friend> {
        Log.d(TAG, "searchFriendsByName: name='$name', limit=$limit")
        return try {
            localFriendsDataSource.searchFriendsByName(name, limit)
        } catch (e: Exception) {
            Log.e(TAG, "searchFriendsByName failed", e)
            emptyList()
        }
    }

    override suspend fun getFriendsByStatus(status: FriendStatus): List<Friend> {
        Log.d(TAG, "getFriendsByStatus: $status")
        return try {
            localFriendsDataSource.getFriendsByStatus(status)
        } catch (e: Exception) {
            Log.e(TAG, "getFriendsByStatus failed", e)
            emptyList()
        }
    }

    override suspend fun getAcceptedFriends(): List<Friend> {
        Log.d(TAG, "getAcceptedFriends")
        return getFriendsByStatus(FriendStatus.ACCEPTED)
    }

    override suspend fun getSentFriendRequests(): List<Friend> {
        Log.d(TAG, "getSentFriendRequests")
        return getFriendsByStatus(FriendStatus.REQUESTED)
    }

    override suspend fun getReceivedFriendRequests(): List<Friend> {
        Log.d(TAG, "getReceivedFriendRequests")
        return getFriendsByStatus(FriendStatus.PENDING)
    }

    override suspend fun getBlockedFriends(): List<Friend> {
        Log.d(TAG, "getBlockedFriends")
        return getFriendsByStatus(FriendStatus.BLOCKED)
    }

    override suspend fun getAllFriends(limit: Int?): List<Friend> {
        Log.d(TAG, "getAllFriends: limit=$limit")
        return try {
            localFriendsDataSource.getAllFriends(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getAllFriends failed", e)
            emptyList()
        }
    }

    override suspend fun getFriendsByIds(friendIds: List<String>): List<Friend> {
        Log.d(TAG, "getFriendsByIds: ${friendIds.size} friends")
        return try {
            localFriendsDataSource.getFriendsByIds(friendIds)
        } catch (e: Exception) {
            Log.e(TAG, "getFriendsByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getFriendsRequestedAfter(timestamp: Instant): List<Friend> {
        Log.d(TAG, "getFriendsRequestedAfter: $timestamp")
        return try {
            getAllFriends().filter { friend ->
                friend.requestedAt?.isAfter(timestamp) == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "getFriendsRequestedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun getFriendsAcceptedAfter(timestamp: Instant): List<Friend> {
        Log.d(TAG, "getFriendsAcceptedAfter: $timestamp")
        return try {
            getAllFriends().filter { friend ->
                friend.acceptedAt?.isAfter(timestamp) == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "getFriendsAcceptedAfter failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveFriend(friend: Friend): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveFriend: ${friend.id}")

            // 1. Room DB에 저장
            localFriendsDataSource.saveFriend(friend)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (friend.isNew) "CREATE" else "UPDATE"
            localFriendsDataSource.addToOutbox(
                friendId = friend.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Friend saved and added to outbox: ${friend.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveFriend failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveFriends(friends: List<Friend>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveFriends: ${friends.size} friends")

            if (friends.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localFriendsDataSource.saveFriends(friends)

            Log.d(TAG, "Bulk friends saved: ${friends.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveFriends failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteFriend(friendId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteFriend: $friendId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localFriendsDataSource.deleteFriend(friendId)

            // 2. Outbox에 삭제 작업 추가
            localFriendsDataSource.addToOutbox(
                friendId = friendId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Friend deleted and added to outbox: $friendId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteFriend failed", e)
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
            Log.d(TAG, "updateFriend: friendId=$friendId")

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

            Log.d(TAG, "Friend updated: $friendId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateFriend failed", e)
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
            Log.e(TAG, "friendExists failed", e)
            false
        }
    }

    override suspend fun nameExists(name: UserName, excludeFriendId: String?): Boolean {
        return try {
            localFriendsDataSource.nameExists(name.value, excludeFriendId)
        } catch (e: Exception) {
            Log.e(TAG, "nameExists failed", e)
            false
        }
    }

    override suspend fun getFriendCountByStatus(status: FriendStatus): Int {
        return try {
            localFriendsDataSource.getFriendCountByStatus(status)
        } catch (e: Exception) {
            Log.e(TAG, "getFriendCountByStatus failed", e)
            0
        }
    }

    override suspend fun getTotalFriendCount(): Int {
        return try {
            localFriendsDataSource.getTotalFriendCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalFriendCount failed", e)
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
            Log.d(TAG, "clearAllFriends")

            localFriendsDataSource.clearAllFriends()

            Log.d(TAG, "All friends cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllFriends failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getFriendsUpdatedAfter(timestamp: Instant): List<Friend> {
        return try {
            localFriendsDataSource.getFriendsUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getFriendsUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        friendId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: friendId=$friendId, operation=$operation")

            localFriendsDataSource.addToOutbox(friendId, operation, payload)

            Log.d(TAG, "Added to outbox: $friendId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}