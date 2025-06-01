package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for project role-related operations.
 */
interface ProjectRoleRepository {
    
    /**
     * Gets a stream of roles for a specific project.
     *
     * @param projectId The ID of the project.
     * @return A [Flow] emitting lists of [Role] objects.
     */
    fun getRolesStream(projectId: String): Flow<List<Role>>
    
    /**
     * Gets a specific role by ID.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to retrieve.
     * @return A [CustomResult] containing the [Role] if found, or an error.
     */
    suspend fun getRole(projectId: String, roleId: String): CustomResult<Role, Exception>
    
    /**
     * Creates a new role in a project.
     *
     * @param projectId The ID of the project.
     * @param name The name of the new role.
     * @param permissions The permissions for the new role.
     * @param isDefault Whether this is a default role.
     * @return A [CustomResult] containing the created [Role], or an error.
     */
    suspend fun createRole(
        projectId: String,
        name: String,
        permissions: Map<String, Boolean>,
        isDefault: Boolean
    ): CustomResult<Role, Exception>
    
    /**
     * Updates an existing role.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to update.
     * @param name The new name for the role (optional).
     * @param permissions The new permissions for the role (optional).
     * @param isDefault The new default status for the role (optional).
     * @return A [CustomResult] containing the updated [Role], or an error.
     */
    suspend fun updateRole(
        projectId: String,
        roleId: String,
        name: String? = null,
        permissions: Map<String, Boolean>? = null,
        isDefault: Boolean? = null
    ): CustomResult<Role, Exception>
    
    /**
     * Deletes a role from a project.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to delete.
     * @return A [CustomResult] indicating success or failure.
     */
    suspend fun deleteRole(projectId: String, roleId: String): CustomResult<Unit, Exception>
}
