package com.example.data_core.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.PermissionsEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface PermissionsDao {

    @Query("SELECT * FROM permissions WHERE id = :permissionId")
    suspend fun getPermissionById(permissionId: String): PermissionsEntity?

    @Query("SELECT * FROM permissions WHERE roleId = :roleId ORDER BY id ASC")
    suspend fun getPermissionsByRole(roleId: String): List<PermissionsEntity>

    @Query("SELECT * FROM permissions WHERE projectId = :projectId ORDER BY roleId, id ASC")
    suspend fun getPermissionsByProject(projectId: String): List<PermissionsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermission(permission: PermissionsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermissions(permissions: List<PermissionsEntity>)

    @Update
    suspend fun updatePermission(permission: PermissionsEntity)

    @Query("DELETE FROM permissions WHERE id = :permissionId")
    suspend fun deletePermission(permissionId: String)

    @Query("SELECT * FROM permissions WHERE roleId = :roleId ORDER BY id ASC")
    fun observePermissionsByRole(roleId: String): Flow<List<PermissionsEntity>>

    @Query("SELECT * FROM permissions WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getPermissionsUpdatedAfter(timestamp: Instant): List<PermissionsEntity>


    @Query("SELECT COUNT(*) FROM permissions WHERE roleId = :roleId")
    suspend fun getPermissionCountByRole(roleId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM permissions WHERE roleId = :roleId AND id = :permissionId)")
    suspend fun hasRolePermission(roleId: String, permissionId: String): Boolean

    @Query("DELETE FROM permissions WHERE roleId = :roleId")
    suspend fun deletePermissionsByRole(roleId: String)

    @Query("DELETE FROM permissions WHERE projectId = :projectId")
    suspend fun deletePermissionsByProject(projectId: String)

    @Query("DELETE FROM permissions")
    suspend fun deleteAllPermissions()
}