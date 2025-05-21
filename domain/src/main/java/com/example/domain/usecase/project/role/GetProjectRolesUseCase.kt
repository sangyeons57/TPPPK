package com.example.domain.usecase.project.role

import com.example.domain.model.Role
import com.example.domain.model.project.RoleSortOption
import kotlinx.coroutines.flow.Flow

/**
 * UseCase for retrieving a list of project roles with optional filtering and sorting.
 */
interface GetProjectRolesUseCase {
    /**
     * Retrieves a flow of project roles, with optional filtering by default status and sorting.
     *
     * @param projectId The ID of the project.
     * @param filterIsDefault Optional. If provided, filters roles where `Role.isDefault` matches this value.
     * @param sortBy Optional. Specifies the sorting order for the roles.
     * @return A [Flow] emitting a list of [Role] objects.
     */
    operator fun invoke(
        projectId: String,
        filterIsDefault: Boolean? = null,
        sortBy: RoleSortOption? = null
    ): Flow<List<Role>>
}
