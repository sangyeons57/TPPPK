package com.example.domain.usecase.project.role

import com.example.domain.model.RolePermission
import kotlin.Result

/**
 * UseCase for updating a project role.
 */
interface UpdateProjectRoleUseCase {
    /**
     * Updates an existing role within a project.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to update.
     * @param name Optional. The new name for the role. If null, the name is not changed.
     * @param permissions Optional. The new permissions map for the role. If null, permissions are not changed.
     * @param isDefault Optional. The new default status for the role. If null, the default status is not changed.
     * @return A [Result] indicating success or failure.
     */
    suspend operator fun invoke(
        projectId: String,
        roleId: String,
        name: String? = null,
        permissions: Map<RolePermission, Boolean>? = null,
        isDefault: Boolean? = null
    ): Result<Unit>
}
