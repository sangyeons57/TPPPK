package com.example.data.datasource.local

import com.example.data.dao.MembersDao
import com.example.data.dao.OutboxDao
import com.example.data.dao.SyncMetadataDao
import com.example.data.mapper.MembersMapper
import com.example.data.model.local.OutboxEntity
import com.example.domain.model.base.Member
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
    private val syncMetadataDao: SyncMetadataDao
) : LocalMembersDataSource {
    companion object {
        private const val COLLECTION_NAME = "members"
    }

    override suspend fun getMemberById(memberId: String): Member? =
        membersDao.getMemberById(memberId)?.let { MembersMapper.toDomain(it) }

    override suspend fun getMembersByProject(projectId: String): List<Member> =
        MembersMapper.toDomainList(membersDao.getMembersByProject(projectId))

    override suspend fun getMemberByProjectAndUser(projectId: String, userId: String): Member? =
        membersDao.getMemberByProjectAndUser(projectId, userId)?.let { MembersMapper.toDomain(it) }

    override suspend fun getAllMembers(): List<Member> =
        MembersMapper.toDomainList(membersDao.getAllMembers())

    override suspend fun saveMember(member: Member) =
        membersDao.insertMember(MembersMapper.toEntity(member))

    override suspend fun saveMembers(members: List<Member>) {
        if (members.isNotEmpty()) membersDao.insertMembers(MembersMapper.toEntityList(members))
    }

    override suspend fun deleteMember(memberId: String) = membersDao.deleteMember(memberId)
    override suspend fun getMembersUpdatedAfter(timestamp: Instant): List<Member> =
        MembersMapper.toDomainList(membersDao.getMembersUpdatedAfter(timestamp))

    override fun observeMembersByProject(projectId: String): Flow<List<Member>> =
        membersDao.observeMembersByProject(projectId).map { MembersMapper.toDomainList(it) }

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
}