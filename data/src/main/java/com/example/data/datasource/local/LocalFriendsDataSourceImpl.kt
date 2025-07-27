package com.example.data.datasource.local

import com.example.data.dao.FriendsDao
import com.example.data.dao.SyncMetadataDao
import com.example.data.dao.OutboxDao
import com.example.data.mapper.FriendsMapper
import com.example.data.model.local.OutboxEntity
import com.example.domain.model.base.Friend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFriendsDataSourceImpl @Inject constructor(
    private val friendsDao: FriendsDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao
) : LocalFriendsDataSource {

    companion object {
        private const val COLLECTION_NAME = "friends"
    }

    override suspend fun getFriendshipById(friendshipId: String): Friend? {
        val entity = friendsDao.getFriendshipById(friendshipId) ?: return null
        return FriendsMapper.toDomain(entity)
    }

    override suspend fun getFriendsByUser(userId: String): List<Friend> {
        val entities = friendsDao.getFriendsByUser(userId)
        return FriendsMapper.toDomainList(entities)
    }

    override suspend fun getFriendshipBetweenUsers(user1Id: String, user2Id: String): Friend? {
        val entity = friendsDao.getFriendshipBetweenUsers(user1Id, user2Id) ?: return null
        return FriendsMapper.toDomain(entity)
    }

    override suspend fun getFriendsByStatus(userId: String, status: String): List<Friend> {
        val entities = friendsDao.getFriendsByStatus(userId, status)
        return FriendsMapper.toDomainList(entities)
    }

    override suspend fun getPendingFriendRequests(userId: String): List<Friend> {
        val entities = friendsDao.getPendingFriendRequests(userId)
        return FriendsMapper.toDomainList(entities)
    }

    override suspend fun getReceivedFriendRequests(userId: String): List<Friend> {
        val entities = friendsDao.getReceivedFriendRequests(userId)
        return FriendsMapper.toDomainList(entities)
    }

    override suspend fun getOnlineFriends(userId: String): List<Friend> {
        val entities = friendsDao.getOnlineFriends(userId)
        return FriendsMapper.toDomainList(entities)
    }

    override suspend fun getAllFriendships(): List<Friend> {
        val entities = friendsDao.getAllFriendships()
        return FriendsMapper.toDomainList(entities)
    }

    override suspend fun saveFriendship(friend: Friend) {
        val entity = FriendsMapper.toEntity(friend)
        friendsDao.insertFriendship(entity)
    }

    override suspend fun saveFriendships(friends: List<Friend>) {
        if (friends.isEmpty()) return
        val entities = FriendsMapper.toEntityList(friends)
        friendsDao.insertFriendships(entities)
    }

    override suspend fun deleteFriendship(friendshipId: String) {
        friendsDao.deleteFriendship(friendshipId)
    }

    override suspend fun deleteFriendshipsByUser(userId: String) {
        friendsDao.deleteFriendshipsByUser(userId)
    }

    override suspend fun updateFriendshipStatus(friendshipId: String, status: String) {
        friendsDao.updateFriendshipStatus(friendshipId, status)
    }

    override suspend fun getFriendshipsUpdatedAfter(timestamp: Instant): List<Friend> {
        val entities = friendsDao.getFriendshipsUpdatedAfter(timestamp)
        return FriendsMapper.toDomainList(entities)
    }

    override fun observeFriendsByUser(userId: String): Flow<List<Friend>> {
        return friendsDao.observeFriendsByUser(userId).map { entities ->
            FriendsMapper.toDomainList(entities)
        }
    }

    override fun observeFriendshipById(friendshipId: String): Flow<Friend?> {
        return friendsDao.observeFriendshipById(friendshipId).map { entity ->
            entity?.let { FriendsMapper.toDomain(it) }
        }
    }

    override fun observeFriendsByStatus(userId: String, status: String): Flow<List<Friend>> {
        return friendsDao.observeFriendsByStatus(userId, status).map { entities ->
            FriendsMapper.toDomainList(entities)
        }
    }

    override fun observePendingFriendRequests(userId: String): Flow<List<Friend>> {
        return friendsDao.observePendingFriendRequests(userId).map { entities ->
            FriendsMapper.toDomainList(entities)
        }
    }

    override suspend fun addToOutbox(friendshipId: String, operation: String, payload: String?) {
        val outboxEntity = OutboxEntity(
            id = UUID.randomUUID().toString(),
            collectionName = COLLECTION_NAME,
            entityId = friendshipId,
            operation = operation,
            payload = payload,
            localTimestamp = System.currentTimeMillis(),
            retries = 0
        )
        outboxDao.insertOperation(outboxEntity)
    }

    override suspend fun getPendingOutboxOperations(): List<FriendOutboxOperation> {
        val entities = outboxDao.getPendingOperationsByCollection(COLLECTION_NAME)
        return entities.map { entity ->
            FriendOutboxOperation(
                id = entity.id,
                friendshipId = entity.entityId,
                operation = entity.operation,
                payload = entity.payload,
                localTimestamp = entity.localTimestamp,
                retries = entity.retries
            )
        }
    }

    override suspend fun markOutboxOperationComplete(operationId: String) {
        outboxDao.deleteOperation(operationId)
    }

    override suspend fun incrementOutboxRetries(operationId: String) {
        outboxDao.incrementRetries(operationId, System.currentTimeMillis())
    }

    override suspend fun getLastSyncCursor(): Long? {
        return syncMetadataDao.getLastServerCursor(COLLECTION_NAME)
    }

    override suspend fun updateSyncCursor(cursor: Long, timestamp: Long) {
        if (!syncMetadataDao.syncMetadataExists(COLLECTION_NAME)) {
            syncMetadataDao.initializeSyncMetadata(COLLECTION_NAME)
        }
        syncMetadataDao.updateSyncStatus(COLLECTION_NAME, cursor, timestamp)
    }

    override suspend fun friendshipExists(friendshipId: String): Boolean {
        return friendsDao.friendshipExists(friendshipId)
    }

    override suspend fun areFriends(user1Id: String, user2Id: String): Boolean {
        return friendsDao.areFriends(user1Id, user2Id)
    }

    override suspend fun getFriendCount(userId: String): Int {
        return friendsDao.getFriendCount(userId)
    }

    override suspend fun getTotalFriendshipCount(): Int {
        return friendsDao.getTotalFriendshipCount()
    }

    override suspend fun getPendingFriendRequestCount(userId: String): Int {
        return friendsDao.getPendingFriendRequestCount(userId)
    }

    override suspend fun getOnlineFriendCount(userId: String): Int {
        return friendsDao.getOnlineFriendCount(userId)
    }

    override suspend fun clearAllFriendships() {
        friendsDao.deleteAllFriendships()
        outboxDao.deleteOperationsByCollection(COLLECTION_NAME)
        syncMetadataDao.deleteSyncMetadata(COLLECTION_NAME)
    }
}