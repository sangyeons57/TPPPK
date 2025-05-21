package com.example.domain.usecase.project.role

import com.example.domain.model.RolePermission
import com.example.domain.repository.ProjectRoleRepository
import javax.inject.Inject
import kotlin.Result

/**
 * Implementation of [CreateProjectRoleUseCase].
 */
class CreateProjectRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository
) : CreateProjectRoleUseCase {

    /**
     * Creates a new role within a project.
     *
     * @param projectId The ID of the project.
     * @param name The name of the new role.
     * @param permissions The map of permissions for the new role.
     * @param isDefault Whether the new role should be a default role. Defaults to false.
     * @return A [Result] containing the ID of the newly created role, or an error.
     */
    override suspend operator fun invoke(
        projectId: String,
        name: String,
        permissions: Map<RolePermission, Boolean>,
        isDefault: Boolean
    ): Result<String> {
        return projectRoleRepository.createRole(projectId, name, permissions, isDefault)
    }
}
