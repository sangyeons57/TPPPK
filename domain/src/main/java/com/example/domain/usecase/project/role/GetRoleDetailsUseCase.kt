package com.example.domain.usecase.project.role

import com.example.domain.model.Role
import com.example.domain.repository.ProjectRoleRepository
import javax.inject.Inject

interface GetRoleDetailsUseCase {
    suspend operator fun invoke(projectId: String, roleId: String): Result<Role?> // Return type changed to Role?
}

class GetRoleDetailsUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository
) : GetRoleDetailsUseCase {
    override suspend operator fun invoke(projectId: String, roleId: String): Result<Role?> { // Return type changed to Role?
        return projectRoleRepository.getRoleDetails(projectId, roleId)
    }
}
