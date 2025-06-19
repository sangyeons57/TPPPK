package com.example.domain.usecase.project.role

import com.example.domain.model.project.RolePermission
import com.example.domain.repository.PermissionRepository
import com.example.domain.repository.RoleRepository
import javax.inject.Inject

/**
 * Use case for fetching the permissions associated with a specific role within a project.
 * Permissions are stored as a subcollection of the role.
 */
interface GetRolePermissionsUseCase {
    /**
     * Fetches the list of permissions for a given role.
     * @param projectId The ID of the project.
     * @param roleId The ID of the role whose permissions are to be fetched.
     * @return A [CustomResult] containing a list of [RolePermission] on success, or an [Exception] on failure.
     */
    suspend operator fun invoke(projectId: String, roleId: String): CustomResult<List<RolePermission>, Exception>
}

/**
 * Implementation of [GetRolePermissionsUseCase].
 * @param roleRepository The repository responsible for role and permission data operations.
 */
class GetRolePermissionsUseCaseImpl @Inject constructor(
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository
) : GetRolePermissionsUseCase {
    /**
     * Fetches the list of permissions for a given role by calling the repository.
     * @param projectId The ID of the project.
     * @param roleId The ID of the role whose permissions are to be fetched.
     * @return A [CustomResult] containing a list of [RolePermission] on success, or an [Exception] on failure.
     */
    override suspend operator fun invoke(projectId: String, roleId: String): CustomResult<List<RolePermission>, Exception> {
        if (projectId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Project ID cannot be blank."))
        }
        if (roleId.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Role ID cannot be blank."))
        }
        return roleRepository.getRolePermissions(projectId, roleId)
    }
}
