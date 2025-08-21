package com.example.domain_usecase.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission
import com.example.domain.vo.CollectionPath
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.PermissionRepository
import javax.inject.Inject

/**
 * UseCase to set (upsert) the enabled permissions for a role.
 * It stores one document per enabled permission under:
 *   /projects/{projectId}/roles/{roleId}/permissions/{permissionId}
 * and removes documents for permissions that are no longer enabled.
 */
interface SetRolePermissionsUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        roleId: DocumentId,
        enabled: List<RolePermission>
    ): CustomResult<Unit, Exception>
}

class SetRolePermissionsUseCaseImpl @Inject constructor(
    private val permissionRepository: PermissionRepository
) : SetRolePermissionsUseCase {

    override suspend fun invoke(
        projectId: DocumentId,
        roleId: DocumentId,
        enabled: List<RolePermission>
    ): CustomResult<Unit, Exception> {
        // Scope repository to role's permission subcollection
        permissionRepository.setCollection(
            CollectionPath.projectRolePermissions(projectId.value, roleId.value)
        )

        // Read current permissions
        val current = when (val res = permissionRepository.findAll()) {
            is CustomResult.Success -> res.data
            is CustomResult.Failure -> return CustomResult.Failure(res.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(res.progress)
        }

        val currentIds = current.map { it.getPermissionRole() }.toSet()
        val targetIds = enabled.toSet()

        val toCreate = targetIds.minus(currentIds)
        val toDelete = currentIds.minus(targetIds)

        // Create missing
        for (perm in toCreate) {
            when (val create = permissionRepository.save(Permission.create(perm))) {
                is CustomResult.Success -> Unit
                is CustomResult.Failure -> return CustomResult.Failure(create.error)
                is CustomResult.Initial -> return CustomResult.Initial
                is CustomResult.Loading -> return CustomResult.Loading
                is CustomResult.Progress -> return CustomResult.Progress(create.progress)
            }
        }

        // Delete removed
        for (perm in toDelete) {
            when (val del = permissionRepository.delete(DocumentId.from(perm))) {
                is CustomResult.Success -> Unit
                is CustomResult.Failure -> return CustomResult.Failure(del.error)
                is CustomResult.Initial -> return CustomResult.Initial
                is CustomResult.Loading -> return CustomResult.Loading
                is CustomResult.Progress -> return CustomResult.Progress(del.progress)
            }
        }

        return CustomResult.Success(Unit)
    }
}

