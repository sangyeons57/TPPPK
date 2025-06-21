package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.RoleRepository
import javax.inject.Inject

interface UpdateRoleUseCase {
    suspend operator fun invoke(projectId: String, roleId: String, newName: String, isDefault: Boolean? = null): CustomResult<Unit, Exception>
}

class UpdateRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: RoleRepository
) : UpdateRoleUseCase {
    override suspend operator fun invoke(
        projectId: String,
        roleId: String,
        newName: String,
        isDefault: Boolean?
    ): CustomResult<Unit, Exception> {
        if (newName.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Role name cannot be blank."))
        }
        return projectRoleRepository.updateRole(
            projectId,
            roleId,
            newName,
            isDefault
        )
    }

}
