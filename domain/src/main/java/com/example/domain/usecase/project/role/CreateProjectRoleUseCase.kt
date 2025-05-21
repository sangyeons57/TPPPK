package com.example.domain.usecase.project.role

import com.example.domain.model.RolePermission
import kotlin.Result

/**
 * UseCase for creating a new project role.
 */
interface CreateProjectRoleUseCase {
    /**
     * Creates a new role within a project.
     *
     * @param projectId The ID of the project.
     * @param name The name of the new role.
     * @param permissions The map of permissions for the new role.
     * @param isDefault Whether the new role should be a default role. Defaults to false.
     * @return A [Result] containing the ID of the newly created role, or an error.
     */
    suspend operator fun invoke(
        projectId: String,
        name: String,
        permissions: Map<RolePermission, Boolean>,
        isDefault: Boolean = false
    ): Result<String>
}
