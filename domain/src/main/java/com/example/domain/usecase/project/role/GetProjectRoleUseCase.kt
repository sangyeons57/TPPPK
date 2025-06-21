package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import com.example.domain.repository.base.RoleRepository
import javax.inject.Inject
import kotlin.Result

/**
 * UseCase for retrieving a specific project role.
 */
interface GetProjectRoleUseCase {
    /**
     * Retrieves a specific role from a project.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to retrieve.
     * @return A [Result] containing the [Role] if found, or null if not found, or an error.
     */
    suspend operator fun invoke(projectId: String, roleId: String): CustomResult<Role, Exception>
}

/**
 * Implementation of [GetProjectRoleUseCase].
 */
class GetProjectRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: RoleRepository
) : GetProjectRoleUseCase {

    /**
     * Retrieves a specific role from a project.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to retrieve.
     * @return A [Result] containing the [Role] if found, or null if not found, or an error.
     */
    override suspend operator fun invoke(projectId: String, roleId: String): CustomResult<Role, Exception> {
        return projectRoleRepository.getRoleDetails(projectId, roleId)
    }
}
