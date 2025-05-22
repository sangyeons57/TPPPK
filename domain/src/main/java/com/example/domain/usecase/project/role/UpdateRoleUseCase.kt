package com.example.domain.usecase.project.role

import com.example.domain.model.RolePermission
import com.example.domain.repository.ProjectRoleRepository
import javax.inject.Inject

interface UpdateRoleUseCase {
    suspend operator fun invoke(projectId: String, roleId: String, newName: String, newPermissions: List<RolePermission>, isDefault: Boolean? = null): Result<Unit>
}

class UpdateRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository
) : UpdateRoleUseCase {
    override suspend operator fun invoke(projectId: String, roleId: String, newName: String, newPermissions: List<RolePermission>, isDefault: Boolean?): Result<Unit> {
        if (newName.isBlank()) {
            return Result.failure(IllegalArgumentException("Role name cannot be blank."))
        }
        return projectRoleRepository.updateRole(projectId, roleId, newName, newPermissions, isDefault)
    }
}
