package com.example.domain.usecase.project.role

import com.example.domain.model.base.Permission
import com.example.domain.repository.RoleRepository
import javax.inject.Inject

interface CreateRoleUseCase {
    suspend operator fun invoke(projectId: String, roleName: String, permissions: List<Permission>, isDefault: Boolean): Result<String> // Return Role ID string
}

class CreateRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: RoleRepository
) : CreateRoleUseCase {
    override suspend operator fun invoke(projectId: String, roleName: String, permissions: List<Permission>, isDefault: Boolean): Result<String> {
        if (roleName.isBlank()) {
            return Result.failure(IllegalArgumentException("Role name cannot be blank."))
        }
        // Add more validation if needed (e.g., for permissions map content)
        return projectRoleRepository.createRole(projectId, roleName, permissions, isDefault)
    }
}
