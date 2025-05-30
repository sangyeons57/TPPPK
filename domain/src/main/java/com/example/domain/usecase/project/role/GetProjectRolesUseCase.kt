package com.example.domain.usecase.project.role

import com.example.domain.model.base.Role
import com.example.domain.repository.RoleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

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
/**
 * Implementation of [GetProjectRolesUseCase].
 */
class GetProjectRolesUseCaseImpl @Inject constructor(
    private val projectRoleRepository: RoleRepository
) : GetProjectRolesUseCase {

    /**
     * Retrieves a flow of project roles, with optional filtering by default status and sorting.
     *
     * @param projectId The ID of the project.
     * @param filterIsDefault Optional. If provided, filters roles where `Role.isDefault` matches this value.
     * @param sortBy Optional. Specifies the sorting order for the roles.
     * @return A [Flow] emitting a list of [Role] objects.
     */
    override operator fun invoke(
        projectId: String,
        filterIsDefault: Boolean?,
        sortBy: RoleSortOption?
    ): Flow<List<Role>> {
        return projectRoleRepository.getRolesStream(projectId).map { roles ->
            val filteredRoles = if (filterIsDefault != null) {
                roles.filter { it.isDefault == filterIsDefault }
            } else {
                roles
            }

            when (sortBy) {
                RoleSortOption.NAME_ASC -> filteredRoles.sortedBy { it.name.lowercase() }
                RoleSortOption.NAME_DESC -> filteredRoles.sortedByDescending { it.name.lowercase() }
                // Add other sort options like MEMBER_COUNT here if Role.memberCount is reliable
                null -> filteredRoles // No sorting
            }
        }
    }
}
