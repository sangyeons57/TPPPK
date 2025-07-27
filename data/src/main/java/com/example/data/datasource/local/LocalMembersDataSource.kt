package com.example.data.datasource.local

import com.example.domain.model.base.Member
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface LocalMembersDataSource {
    suspend fun getMemberById(memberId: String): Member?
    suspend fun getMembersByProject(projectId: String): List<Member>
    suspend fun getMemberByProjectAndUser(projectId: String, userId: String): Member?
    suspend fun getAllMembers(): List<Member>
    suspend fun saveMember(member: Member)
    suspend fun saveMembers(members: List<Member>)
    suspend fun deleteMember(memberId: String)
    suspend fun getMembersUpdatedAfter(timestamp: Instant): List<Member>
    fun observeMembersByProject(projectId: String): Flow<List<Member>>
    suspend fun addToOutbox(memberId: String, operation: String, payload: String? = null)
    suspend fun getPendingOutboxOperations(): List<MemberOutboxOperation>
    suspend fun markOutboxOperationComplete(operationId: String)
    suspend fun incrementOutboxRetries(operationId: String)
    suspend fun getLastSyncCursor(): Long?
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long)
    suspend fun clearAllMembers()
}

data class MemberOutboxOperation(
    val id: String,
    val memberId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)