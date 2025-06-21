package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.RoleRepository
import javax.inject.Inject
import kotlin.Result

/**
 * UseCase for creating a new project role.
 */
interface CreateProjectRoleUseCase {
    /**
     * Creates a new role within a project.
     *
     * @param projectId The ID of the project.
     * @param name The name of the new role.
     * @param permissions The map of permissions for the new role.
     * @param isDefault Whether the new role should be a default role. Defaults to false.
     * @return A [Result] containing the ID of the newly created role, or an error.
     */
    suspend operator fun invoke(
        projectId: String,
        name: String,
        isDefault: Boolean = false
    ): CustomResult<String, Exception>
}

/**
 * Implementation of [CreateProjectRoleUseCase].
 */
class CreateProjectRoleUseCaseImpl @Inject constructor(
    private val roleRepository: RoleRepository
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
        isDefault: Boolean
    ): CustomResult<String, Exception> {
        return roleRepository.createRole(projectId, name, isDefault)
    }
}
