package com.example.domain.usecase.project.member

import com.example.domain.model.ProjectMember
import com.example.domain.model.project.MemberSortOption
import com.example.domain.repository.ProjectMemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [GetProjectMembersUseCase].
 */
class GetProjectMembersUseCaseImpl @Inject constructor(
    private val projectMemberRepository: ProjectMemberRepository
) : GetProjectMembersUseCase {

    /**
     * Retrieves a flow of project members, with optional filtering by role and sorting.
     *
     * @param projectId The ID of the project.
     * @param roleIdFilter Optional. If provided, only members with this role ID will be included.
     * @param sortBy Optional. Specifies the sorting order for the members.
     * @return A [Flow] emitting a list of [ProjectMember] objects.
     */
    override operator fun invoke(
        projectId: String,
        roleIdFilter: String?,
        sortBy: MemberSortOption?
    ): Flow<List<ProjectMember>> {
        return projectMemberRepository.getProjectMembersStream(projectId).map { members ->
            val filteredMembers = if (roleIdFilter != null) {
                members.filter { it.roleIds.contains(roleIdFilter) }
            } else {
                members
            }

            when (sortBy) {
                MemberSortOption.NAME_ASC -> filteredMembers.sortedBy { it.userName.lowercase() }
                MemberSortOption.NAME_DESC -> filteredMembers.sortedByDescending { it.userName.lowercase() }
                MemberSortOption.JOINED_AT_ASC -> filteredMembers.sortedBy { it.joinedAt }
                MemberSortOption.JOINED_AT_DESC -> filteredMembers.sortedByDescending { it.joinedAt }
                null -> filteredMembers // No sorting
            }
        }
    }
}
