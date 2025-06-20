package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.RoleRepository
import javax.inject.Inject
import kotlin.Result

/**
 * UseCase for deleting a project role.
 */
interface DeleteProjectRoleUseCase {
    /**
     * Deletes an existing role from a project.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to delete.
     * @return A [Result] indicating success or failure.
     */
    suspend operator fun invoke(projectId: String, roleId: String): CustomResult<Unit, Exception>
}
/**
 * Implementation of [DeleteProjectRoleUseCase].
 */
class DeleteProjectRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: RoleRepository,
    private val authRepository: AuthRepository
) : DeleteProjectRoleUseCase {

    /**
     * Deletes an existing role from a project.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to delete.
     * @return A [Result] indicating success or failure.
     */
    override suspend operator fun invoke(projectId: String, roleId: String): CustomResult<Unit, Exception> {
        val session = authRepository.getCurrentUserSession()
        return when (session) {
            is CustomResult.Success -> projectRoleRepository.deleteRole(projectId, roleId)
            else -> CustomResult.Failure(Exception("User not authenticated"))
        }
    }
}
