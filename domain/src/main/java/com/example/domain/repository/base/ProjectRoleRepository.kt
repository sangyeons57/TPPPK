package com.example.domain.repository.base

import com.example.domain.model.base.Role
import com.example.domain.repository.DefaultRepository
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for project role-related operations.
 */
interface ProjectRoleRepository : DefaultRepository {
    
    /**
     * Gets a stream of roles for a specific project.
     *
     * @param projectId The ID of the project.
     * @return A [Flow] emitting lists of [Role] objects.
     */
    fun getRolesStream(projectId: String): Flow<List<Role>>
}
