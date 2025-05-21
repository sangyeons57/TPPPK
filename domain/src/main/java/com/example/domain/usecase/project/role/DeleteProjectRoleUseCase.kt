package com.example.domain.usecase.project.role

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
    suspend operator fun invoke(projectId: String, roleId: String): Result<Unit>
}
