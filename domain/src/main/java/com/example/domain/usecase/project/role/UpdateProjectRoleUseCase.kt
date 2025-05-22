package com.example.domain.usecase.project.role

import com.example.domain.model.RolePermission
import com.example.domain.repository.ProjectRoleRepository
import javax.inject.Inject
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
        permissions: List<RolePermission>? = null,
        isDefault: Boolean? = null
    ): Result<Unit>
}
/**
 * Implementation of [UpdateProjectRoleUseCase].
 */
class UpdateProjectRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository
) : UpdateProjectRoleUseCase {

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
    override suspend operator fun invoke(
        projectId: String,
        roleId: String,
        name: String?,
        permissions: List<RolePermission>?,
        isDefault: Boolean?
    ): Result<Unit> {
        // Fetch the current role details to get existing values if not provided
        val currentRoleResult = projectRoleRepository.getRoleDetails(projectId, roleId)

        if (currentRoleResult.isFailure) {
            return Result.failure(currentRoleResult.exceptionOrNull() ?: Exception("Failed to fetch current role details."))
        }

        val currentRole = currentRoleResult.getOrNull()
            ?: return Result.failure(Exception("Role with ID $roleId not found in project $projectId."))

        // Use provided values or fallback to current role's values for name and permissions
        val updatedName = name ?: currentRole.name
        val updatedPermissions = permissions ?: currentRole.permissions

        // The isDefault parameter can be passed directly to the repository as it handles nullable.
        // If name or permissions were null, we use the currentRole's values.
        return projectRoleRepository.updateRole(
            projectId = projectId,
            roleId = roleId,
            name = updatedName,
            permissions = updatedPermissions,
            isDefault = isDefault // Pass isDefault as is, can be null for no change
        )
    }
}
