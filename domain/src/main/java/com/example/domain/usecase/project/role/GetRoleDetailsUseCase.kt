package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import com.example.domain.repository.RoleRepository
import javax.inject.Inject

interface GetRoleDetailsUseCase {
    suspend operator fun invoke(projectId: String, roleId: String): CustomResult<Role, Exception> // Return type changed to Role?
}

class GetRoleDetailsUseCaseImpl @Inject constructor(
    private val projectRoleRepository: RoleRepository
) : GetRoleDetailsUseCase {
    override suspend operator fun invoke(projectId: String, roleId: String): CustomResult<Role, Exception> { // Return type changed to Role?
        return projectRoleRepository.getRoleDetails(projectId, roleId)
    }
}
