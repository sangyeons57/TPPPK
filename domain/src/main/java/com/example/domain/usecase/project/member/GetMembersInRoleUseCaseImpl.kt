package com.example.domain.usecase.project.member

import com.example.domain.model.ProjectMember
import com.example.domain.repository.ProjectMemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [GetMembersInRoleUseCase].
 */
class GetMembersInRoleUseCaseImpl @Inject constructor(
    private val projectMemberRepository: ProjectMemberRepository
) : GetMembersInRoleUseCase {

    /**
     * Retrieves a flow of project members who are assigned the specified role.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to filter members by.
     * @return A [Flow] emitting a list of [ProjectMember] objects assigned to the role.
     */
    override operator fun invoke(projectId: String, roleId: String): Flow<List<ProjectMember>> {
        return projectMemberRepository.getProjectMembersStream(projectId).map { members ->
            members.filter { it.roleIds.contains(roleId) }
        }
    }
}
