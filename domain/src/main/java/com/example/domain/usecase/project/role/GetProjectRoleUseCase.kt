package com.example.domain.usecase.project.role

import com.example.domain.model.Role
import kotlin.Result

/**
 * UseCase for retrieving a specific project role.
 */
interface GetProjectRoleUseCase {
    /**
     * Retrieves a specific role from a project.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to retrieve.
     * @return A [Result] containing the [Role] if found, or null if not found, or an error.
     */
    suspend operator fun invoke(projectId: String, roleId: String): Result<Role?>
}
