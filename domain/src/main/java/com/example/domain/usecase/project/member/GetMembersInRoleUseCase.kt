package com.example.domain.usecase.project.member

import com.example.domain.model.ProjectMember
import kotlinx.coroutines.flow.Flow

/**
 * UseCase for retrieving all members assigned to a specific role within a project.
 */
interface GetMembersInRoleUseCase {
    /**
     * Retrieves a flow of project members who are assigned the specified role.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to filter members by.
     * @return A [Flow] emitting a list of [ProjectMember] objects assigned to the role.
     */
    operator fun invoke(projectId: String, roleId: String): Flow<List<ProjectMember>>
}
