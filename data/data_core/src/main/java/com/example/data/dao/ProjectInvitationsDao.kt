package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.ProjectInvitationsEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ProjectInvitationsDao {

    @Query("SELECT * FROM project_invitations WHERE id = :invitationId")
    suspend fun getInvitationById(invitationId: String): ProjectInvitationsEntity?

    @Query("SELECT * FROM project_invitations WHERE inviteCode = :inviteCode")
    suspend fun getInvitationByCode(inviteCode: String): ProjectInvitationsEntity?

    @Query("SELECT * FROM project_invitations WHERE projectId = :projectId ORDER BY createdAt DESC")
    suspend fun getInvitationsByProject(projectId: String): List<ProjectInvitationsEntity>

    @Query("SELECT * FROM project_invitations WHERE inviterId = :inviterId ORDER BY createdAt DESC")
    suspend fun getInvitationsByInviter(inviterId: String): List<ProjectInvitationsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitation(invitation: ProjectInvitationsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitations(invitations: List<ProjectInvitationsEntity>)

    @Update
    suspend fun updateInvitation(invitation: ProjectInvitationsEntity)

    @Query("DELETE FROM project_invitations WHERE id = :invitationId")
    suspend fun deleteInvitation(invitationId: String)

    @Query("SELECT * FROM project_invitations WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeInvitationsByProject(projectId: String): Flow<List<ProjectInvitationsEntity>>

    @Query("SELECT * FROM project_invitations WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getInvitationsByStatus(status: String): List<ProjectInvitationsEntity>

    @Query("SELECT * FROM project_invitations WHERE status = 'ACTIVE' AND (expiresAt IS NULL OR expiresAt > :currentTime)")
    suspend fun getActiveInvitations(currentTime: Instant): List<ProjectInvitationsEntity>

    @Query("SELECT * FROM project_invitations WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getInvitationsUpdatedAfter(timestamp: Instant): List<ProjectInvitationsEntity>


    @Query("UPDATE project_invitations SET status = :newStatus WHERE id = :invitationId")
    suspend fun updateInvitationStatus(invitationId: String, newStatus: String)

    @Query("SELECT COUNT(*) FROM project_invitations WHERE projectId = :projectId")
    suspend fun getInvitationCountByProject(projectId: String): Int

    @Query("SELECT COUNT(*) FROM project_invitations WHERE projectId = :projectId AND status = :status")
    suspend fun getInvitationCountByProjectAndStatus(projectId: String, status: String): Int

    @Query("DELETE FROM project_invitations WHERE projectId = :projectId")
    suspend fun deleteInvitationsByProject(projectId: String)

    @Query("DELETE FROM project_invitations WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredInvitations(currentTime: Instant): Int

    @Query("DELETE FROM project_invitations")
    suspend fun deleteAllInvitations()
}