package com.example.data_core.datasource.local

import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.dao.UsersDao
import com.example.data_core.model.local.OutboxEntity
import com.example.domain.model.base.User
import com.example.domain.model.vo.user.UserName
import com.example.mapper.UserEntityMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 사용자 데이터 저장소 구현체
 * Room Database를 사용하여 3-tier 클라이언트 주도 동기화를 제공합니다
 */
@Singleton
class LocalUsersDataSourceImpl @Inject constructor(
    private val usersDao: UsersDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val mapper: UserEntityMapper
) : LocalUsersDataSource {

    companion object {
        private const val COLLECTION_NAME = "users"
    }

    // === 기본 CRUD 작업 ===

    override suspend fun getUserById(userId: String): User? {
        val entity = usersDao.getUserById(userId) ?: return null
        return mapper.toDomain(entity)
    }

    override suspend fun getAllUsers(): List<User> {
        val entities = usersDao.getAllUsers()
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun getUserByEmail(email: String): User? {
        val entity = usersDao.getUserByEmail(email) ?: return null
        return mapper.toDomain(entity)
    }

    override suspend fun getUsersByAccountStatus(accountStatus: String): List<User> {
        val entities = usersDao.getUsersByAccountStatus(accountStatus)
        return entities.map { mapper.toDomain(it) }
    }

    override suspend fun saveUser(user: User) {
        val entity = mapper.toEntity(user)
        usersDao.insertUser(entity)
    }

    override suspend fun saveUsers(users: List<User>) {
        if (users.isEmpty()) return

        val entities = users.map { mapper.toEntity(it) }
        usersDao.insertUsers(entities)
    }

    override suspend fun deleteUser(userId: String) {
        usersDao.deleteUser(userId)
    }

    // === 3-tier 동기화 지원 ===

    override suspend fun getUsersUpdatedAfter(timestamp: Instant): List<User> {
        val entities = usersDao.getUsersUpdatedAfter(timestamp)
        return entities.map { mapper.toDomain(it) }
    }

    override fun observeUserById(userId: String): Flow<User?> {
        return usersDao.observeUserById(userId).map { entity ->
            entity?.let { mapper.toDomain(it) }
        }
    }

    override fun observeAllUsers(): Flow<List<User>> {
        return usersDao.observeAllUsers().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    // === Outbox 관리 ===

    override suspend fun addToOutbox(userId: String, operation: String, payload: String?) {
        val outboxEntity = OutboxEntity(
            id = UUID.randomUUID().toString(),
            collectionName = COLLECTION_NAME,
            entityId = userId,
            operation = operation,
            payload = payload,
            localTimestamp = System.currentTimeMillis(),
            retries = 0
        )
        outboxDao.insertOperation(outboxEntity)
    }

    override suspend fun getPendingOutboxOperations(): List<UserOutboxOperation> {
        val entities = outboxDao.getPendingOperationsByCollection(COLLECTION_NAME)
        return entities.map { entity ->
            UserOutboxOperation(
                id = entity.id,
                userId = entity.entityId,
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

    // === 동기화 메타데이터 관리 ===

    override suspend fun getLastSyncCursor(): Long? {
        return syncMetadataDao.getLastServerCursor(COLLECTION_NAME)
    }

    override suspend fun updateSyncCursor(cursor: Long, timestamp: Long) {
        if (!syncMetadataDao.syncMetadataExists(COLLECTION_NAME)) {
            syncMetadataDao.initializeSyncMetadata(COLLECTION_NAME)
        }
        syncMetadataDao.updateSyncStatus(COLLECTION_NAME, cursor, timestamp)
    }

    // === 유틸리티 ===

    override suspend fun userExists(userId: String): Boolean {
        return usersDao.userExists(userId)
    }

    override suspend fun getUserCount(): Int {
        return usersDao.getUserCount()
    }

    override suspend fun clearAllUsers() {
        usersDao.deleteAllUsers()
        outboxDao.deleteOperationsByCollection(COLLECTION_NAME)
        syncMetadataDao.deleteSyncMetadata(COLLECTION_NAME)
    }

    override fun observeByName(name: UserName): Flow<User?> {
        return usersDao.observeByName(name.value).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeAllByName(name: String, limit: Int): Flow<List<User>> {
        return usersDao.observeAllByName(name, limit).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeByEmail(email: String): Flow<User?> {
        return usersDao.observeByEmail(email).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeUsers(userIds: List<String>): Flow<List<User>> {
        return usersDao.observeUsers(userIds).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeUserUpdatedAt(userId: String): Flow<Long?> {
        return usersDao.observeUserById(userId).map { it?.updatedAt?.toEpochMilli() }
    }

    override suspend fun getUserByEmail(email: String): User? {
        return usersDao.getUserByEmail(email)?.let { mapper.toDomain(it) }
    }

    override suspend fun getUserByName(name: UserName): User? {
        return usersDao.getUserByName(name.value)?.let { mapper.toDomain(it) }
    }

    override suspend fun searchUsersByName(name: String, limit: Int): List<User> {
        return usersDao.searchUsersByName(name, limit).map { mapper.toDomain(it) }
    }

    override suspend fun getUsersByIds(userIds: List<String>): List<User> {
        return usersDao.getUsersByIds(userIds).map { mapper.toDomain(it) }
    }

    override suspend fun getUsersByAccountStatus(accountStatus: String): List<User> {
        return usersDao.getUsersByAccountStatus(accountStatus).map { mapper.toDomain(it) }
    }

    override suspend fun emailExists(email: String): Boolean {
        return usersDao.emailExists(email)
    }

    override suspend fun nameExists(name: UserName): Boolean {
        return usersDao.nameExists(name.value)
    }

    override suspend fun getTotalUserCount(): Int {
        return usersDao.getTotalUserCount()
    }

    override suspend fun getActiveUserCount(): Int {
        return usersDao.getActiveUserCount()
    }
