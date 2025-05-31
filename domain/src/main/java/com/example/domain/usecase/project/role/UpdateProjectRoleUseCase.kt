package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Permission
import com.example.domain.repository.RoleRepository
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
        permissions: List<Permission>? = null,
        isDefault: Boolean? = null
    ): CustomResult<Unit, Exception>
}
/**
 * Implementation of [UpdateProjectRoleUseCase].
 */
class UpdateProjectRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: RoleRepository
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
        permissions: List<Permission>?,
        isDefault: Boolean?
    ): CustomResult<Unit, Exception> {
        // Fetch the current role details to get existing values if not provided
        val currentRoleResult = projectRoleRepository.getRoleDetails(projectId, roleId)

        return when (currentRoleResult) {
            is CustomResult.Success -> {
                val currentRole = currentRoleResult.data

                // Use provided values or fallback to current role's values for name and permissions
                val updatedName = name ?: currentRole.name

                // The isDefault parameter can be passed directly to the repository as it handles nullable.
                // If name or permissions were null, we use the currentRole's values.
                projectRoleRepository.updateRole(
                    projectId = projectId,
                    roleId = roleId,
                    name = updatedName,
                    isDefault = isDefault // Pass isDefault as is, can be null for no change
                )
            }
            is CustomResult.Failure -> return CustomResult.Failure(currentRoleResult.error)
            else -> CustomResult.Failure(Exception("Role with ID $roleId not found in project $projectId."))
        }
    }
}
