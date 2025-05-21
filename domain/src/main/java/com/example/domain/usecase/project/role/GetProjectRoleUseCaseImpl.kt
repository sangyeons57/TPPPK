package com.example.domain.usecase.project.role

import com.example.domain.model.Role
import com.example.domain.repository.ProjectRoleRepository
import javax.inject.Inject
import kotlin.Result

/**
 * Implementation of [GetProjectRoleUseCase].
 */
class GetProjectRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository
) : GetProjectRoleUseCase {

    /**
     * Retrieves a specific role from a project.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to retrieve.
     * @return A [Result] containing the [Role] if found, or null if not found, or an error.
     */
    override suspend operator fun invoke(projectId: String, roleId: String): Result<Role?> {
        return projectRoleRepository.getRoleDetails(projectId, roleId)
    }
}
