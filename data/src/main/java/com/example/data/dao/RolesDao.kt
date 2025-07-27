package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.local.RolesEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface RolesDao {

    @Query("SELECT * FROM roles WHERE id = :roleId")
    suspend fun getRoleById(roleId: String): RolesEntity?

    @Query("SELECT * FROM roles WHERE projectId = :projectId ORDER BY name ASC")
    suspend fun getRolesByProject(projectId: String): List<RolesEntity>

    @Query("SELECT * FROM roles WHERE projectId = :projectId AND isDefault = 1")
    suspend fun getDefaultRoleByProject(projectId: String): RolesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRole(role: RolesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoles(roles: List<RolesEntity>)

    @Update
    suspend fun updateRole(role: RolesEntity)

    @Query("DELETE FROM roles WHERE id = :roleId")
    suspend fun deleteRole(roleId: String)

    @Query("SELECT * FROM roles WHERE projectId = :projectId ORDER BY name ASC")
    fun observeRolesByProject(projectId: String): Flow<List<RolesEntity>>

    @Query("SELECT * FROM roles WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getRolesUpdatedAfter(timestamp: Instant): List<RolesEntity>


    @Query("SELECT COUNT(*) FROM roles WHERE projectId = :projectId")
    suspend fun getRoleCountByProject(projectId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM roles WHERE id = :roleId)")
    suspend fun roleExists(roleId: String): Boolean

    @Query("DELETE FROM roles WHERE projectId = :projectId")
    suspend fun deleteRolesByProject(projectId: String)

    @Query("DELETE FROM roles")
    suspend fun deleteAllRoles()
}