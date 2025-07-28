package com.example.data_core.datasource.local

import com.example.data_core.dao.MembersDao
import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_core.model.local.OutboxEntity
import com.example.domain.model.base.Member
import com.example.mapper.MemberEntityMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMembersDataSourceImpl @Inject constructor(
    private val membersDao: MembersDao,
    private val outboxDao: OutboxDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val mapper: MemberEntityMapper
) : LocalMembersDataSource {
    companion object {
        private const val COLLECTION_NAME = "members"
    }

    override suspend fun getMemberById(memberId: String): Member? =
        membersDao.getMemberById(memberId)?.let { mapper.toDomain(it) }

    override suspend fun getMembersByProject(projectId: String): List<Member> =
        membersDao.getMembersByProject(projectId).map { mapper.toDomain(it) }

    override suspend fun getMemberByProjectAndUser(projectId: String, userId: String): Member? =
        membersDao.getMemberByProjectAndUser(projectId, userId)?.let { mapper.toDomain(it) }

    override suspend fun getAllMembers(): List<Member> =
        membersDao.getAllMembers().map { mapper.toDomain(it) }

    override suspend fun saveMember(member: Member) =
        membersDao.insertMember(mapper.toEntity(member))

    override suspend fun saveMembers(members: List<Member>) {
        if (members.isNotEmpty()) membersDao.insertMembers(members.map { mapper.toEntity(it) })
    }

    override suspend fun deleteMember(memberId: String) = membersDao.deleteMember(memberId)
    override suspend fun getMembersUpdatedAfter(timestamp: Instant): List<Member> =
        membersDao.getMembersUpdatedAfter(timestamp).map { mapper.toDomain(it) }

    override fun observeMembersByProject(projectId: String): Flow<List<Member>> =
        membersDao.observeMembersByProject(projectId).map { it.map { entity -> mapper.toDomain(entity) } }

    override suspend fun addToOutbox(memberId: String, operation: String, payload: String?) =
        outboxDao.insertOperation(
            OutboxEntity(
                UUID.randomUUID().toString(),
                COLLECTION_NAME,
                memberId,
                operation,
                payload,
                System.currentTimeMillis(),
                0
            )
        )

    override suspend fun getPendingOutboxOperations(): List<MemberOutboxOperation> =
        outboxDao.getPendingOperationsByCollection(COLLECTION_NAME).map {
            MemberOutboxOperation(
                it.id,
                it.entityId,
                it.operation,
                it.payload,
                it.localTimestamp,
                it.retries
            )
        }

    override suspend fun markOutboxOperationComplete(operationId: String) =
        outboxDao.deleteOperation(operationId)

    override suspend fun incrementOutboxRetries(operationId: String) =
        outboxDao.incrementRetries(operationId, System.currentTimeMillis())

    override suspend fun getLastSyncCursor(): Long? =
        syncMetadataDao.getLastServerCursor(COLLECTION_NAME)

    override suspend fun updateSyncCursor(cursor: Long, timestamp: Long) {
        if (!syncMetadataDao.syncMetadataExists(COLLECTION_NAME)) syncMetadataDao.initializeSyncMetadata(
            COLLECTION_NAME
        ); syncMetadataDao.updateSyncStatus(COLLECTION_NAME, cursor, timestamp)
    }

    override suspend fun clearAllMembers() {
        membersDao.deleteAllMembers(); outboxDao.deleteOperationsByCollection(COLLECTION_NAME); syncMetadataDao.deleteSyncMetadata(
            COLLECTION_NAME
        )
    }

    override fun observeMemberById(memberId: String): Flow<Member?> {
        return membersDao.observeMemberById(memberId).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeMemberByProjectAndUser(projectId: String, userId: String): Flow<Member?> {
        return membersDao.observeMemberByProjectAndUser(projectId, userId).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeMembers(memberIds: List<String>): Flow<List<Member>> {
        return membersDao.observeMembers(memberIds).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeMembersByRole(roleId: String): Flow<List<Member>> {
        return membersDao.observeMembersByRole(roleId).map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override fun observeMemberUpdatedAt(memberId: String): Flow<Long?> {
        return membersDao.observeMemberById(memberId).map { it?.updatedAt?.toEpochMilli() }
    }

    override fun observeAllMembers(): Flow<List<Member>> {
        return membersDao.observeAllMembers().map { it.map { entity -> mapper.toDomain(entity) } }
    }

    override suspend fun getMembersByIds(memberIds: List<String>): List<Member> {
        return membersDao.getMembersByIds(memberIds).map { mapper.toDomain(it) }
    }

    override suspend fun getMembersByRole(roleId: String): List<Member> {
        return membersDao.getMembersByRole(roleId).map { mapper.toDomain(it) }
    }

    override suspend fun getMembersByRoles(roleIds: List<String>): List<Member> {
        return membersDao.getMembersByRoles(roleIds).map { mapper.toDomain(it) }
    }

    override suspend fun memberExists(memberId: String): Boolean {
        return membersDao.memberExists(memberId)
    }

    override suspend fun isUserMemberOfProject(projectId: String, userId: String): Boolean {
        return membersDao.isUserMemberOfProject(projectId, userId)
    }

    override suspend fun memberHasRole(memberId: String, roleId: String): Boolean {
        return membersDao.memberHasRole(memberId, roleId)
    }

    override suspend fun getTotalMemberCount(): Int {
        return membersDao.getTotalMemberCount()
    }

    override suspend fun getMemberCountByProject(projectId: String): Int {
        return membersDao.getMemberCountByProject(projectId)
    }

    override suspend fun getMemberCountByRole(roleId: String): Int {
        return membersDao.getMemberCountByRole(roleId)
    }
}