package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.model.project.RolePermission
import com.example.domain.repository.base.RoleRepository
import javax.inject.Inject

/**
 * Use case for setting or updating the permissions for a specific role within a project.
 * This involves writing to the permissions subcollection of the role.
 */
interface SetRolePermissionsUseCase {
    /**
     * Sets or updates the permissions for a given role.
     * @param projectId The ID of the project.
     * @param roleId The ID of the role whose permissions are to be set.
     * @param permissions The list of [RolePermission] to set for the role.
     * @return A [CustomResult] indicating success ([Unit]) or failure ([Exception]).
     */
    suspend operator fun invoke(projectId: String, roleId: String, permissions: List<RolePermission>): CustomResult<Unit, Exception>
}

/**
 * Implementation of [SetRolePermissionsUseCase].
 * @param roleRepository The repository responsible for role and permission data operations.
 */
class SetRolePermissionsUseCaseImpl @Inject constructor(
    private val roleRepository: RoleRepository
) : SetRolePermissionsUseCase {
    /**
     * Sets or updates the permissions for a given role by calling the repository.
     * @param projectId The ID of the project.
     * @param roleId The ID of the role whose permissions are to be set.
     * @param permissions The list of [RolePermission] to set for the role.
     * @return A [CustomResult] indicating success ([Unit]) or failure ([Exception]).
     */
    override suspend operator fun invoke(projectId: String, roleId: String, permissions: List<RolePermission>): CustomResult<Unit, Exception> {
        if (projectId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Project ID cannot be blank."))
        }
        if (roleId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Role ID cannot be blank."))
        }
        // Consider adding validation for the permissions list itself (e.g., not empty, valid content)
        return roleRepository.setRolePermissions(projectId, roleId, permissions)
    }
}
