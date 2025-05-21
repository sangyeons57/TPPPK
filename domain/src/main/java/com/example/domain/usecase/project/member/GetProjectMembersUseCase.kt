package com.example.domain.usecase.project.member

import com.example.domain.model.ProjectMember
import com.example.domain.model.project.MemberSortOption
import kotlinx.coroutines.flow.Flow

/**
 * UseCase for retrieving a list of project members with optional filtering and sorting.
 */
interface GetProjectMembersUseCase {
    /**
     * Retrieves a flow of project members, with optional filtering by role and sorting.
     *
     * @param projectId The ID of the project.
     * @param roleIdFilter Optional. If provided, only members with this role ID will be included.
     * @param sortBy Optional. Specifies the sorting order for the members.
     * @return A [Flow] emitting a list of [ProjectMember] objects.
     */
    operator fun invoke(
        projectId: String,
        roleIdFilter: String? = null,
        sortBy: MemberSortOption? = null
    ): Flow<List<ProjectMember>>
}
