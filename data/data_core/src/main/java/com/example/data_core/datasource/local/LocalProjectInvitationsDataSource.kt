package com.example.data_core.datasource.local

import com.example.domain.model.base.ProjectInvitation
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface LocalProjectInvitationsDataSource {
    suspend fun getInvitationById(invitationId: String): ProjectInvitation?
    suspend fun getInvitationsByProject(projectId: String): List<ProjectInvitation>
    suspend fun getInvitationsByUser(userId: String): List<ProjectInvitation>
    suspend fun getAllInvitations(): List<ProjectInvitation>
    suspend fun saveInvitation(invitation: ProjectInvitation)
    suspend fun saveInvitations(invitations: List<ProjectInvitation>)
    suspend fun deleteInvitation(invitationId: String)
    suspend fun getInvitationsUpdatedAfter(timestamp: Instant): List<ProjectInvitation>
    fun observeInvitationsByProject(projectId: String): Flow<List<ProjectInvitation>>
    suspend fun addToOutbox(invitationId: String, operation: String, payload: String? = null)
    suspend fun getPendingOutboxOperations(): List<ProjectInvitationOutboxOperation>
    suspend fun markOutboxOperationComplete(operationId: String)
    suspend fun incrementOutboxRetries(operationId: String)
    suspend fun getLastSyncCursor(): Long?
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long)
    suspend fun clearAllInvitations()
}

data class ProjectInvitationOutboxOperation(
    val id: String,
    val invitationId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)