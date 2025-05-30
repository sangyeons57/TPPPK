package com.example.domain.usecase.project.member

import com.example.domain.model.base.Member
import com.example.domain.repository.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

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
    operator fun invoke(projectId: String, roleId: String): Flow<List<Member>>
}
/**
 * Implementation of [GetMembersInRoleUseCase].
 */
class GetMembersInRoleUseCaseImpl @Inject constructor(
    private val projectMemberRepository: MemberRepository
) : GetMembersInRoleUseCase {

    /**
     * Retrieves a flow of project members who are assigned the specified role.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to filter members by.
     * @return A [Flow] emitting a list of [ProjectMember] objects assigned to the role.
     */
    override operator fun invoke(projectId: String, roleId: String): Flow<List<Member>> {
        return projectMemberRepository.getProjectMembersStream(projectId).map { members ->
            members.filter { member -> member.roles.any { role -> role.id == roleId } }
        }
    }
}
