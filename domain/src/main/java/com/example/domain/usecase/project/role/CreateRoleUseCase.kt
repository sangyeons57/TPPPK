package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.RoleRepository
import javax.inject.Inject

interface CreateRoleUseCase {
    suspend operator fun invoke(projectId: String, roleName: String, isDefault: Boolean): CustomResult<String, Exception> // Return Role ID string
}

class CreateRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: RoleRepository
) : CreateRoleUseCase {
    override suspend operator fun invoke(projectId: String, roleName: String, isDefault: Boolean): CustomResult<String, Exception> {
        if (roleName.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Role name cannot be blank."))
        }
        // Add more validation if needed (e.g., for permissions map content)
        return projectRoleRepository.createRole(projectId, roleName, isDefault)
    }
}
