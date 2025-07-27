package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.local.MembersEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface MembersDao {

    @Query("SELECT * FROM members WHERE id = :memberId")
    suspend fun getMemberById(memberId: String): MembersEntity?

    @Query("SELECT * FROM members WHERE projectId = :projectId ORDER BY createdAt ASC")
    suspend fun getMembersByProject(projectId: String): List<MembersEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MembersEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MembersEntity>)

    @Update
    suspend fun updateMember(member: MembersEntity)

    @Query("DELETE FROM members WHERE id = :memberId")
    suspend fun deleteMember(memberId: String)

    @Query("SELECT * FROM members WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun observeMembersByProject(projectId: String): Flow<List<MembersEntity>>

    @Query("SELECT * FROM members WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getMembersUpdatedAfter(timestamp: Instant): List<MembersEntity>


    @Query("SELECT COUNT(*) FROM members WHERE projectId = :projectId")
    suspend fun getMemberCountByProject(projectId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM members WHERE id = :userId AND projectId = :projectId)")
    suspend fun isMemberOfProject(userId: String, projectId: String): Boolean

    @Query("DELETE FROM members WHERE projectId = :projectId")
    suspend fun deleteMembersByProject(projectId: String)

    @Query("DELETE FROM members")
    suspend fun deleteAllMembers()
}