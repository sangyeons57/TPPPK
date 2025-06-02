package com.example.domain.usecase.project.role

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import com.example.domain.model.ui.project.RoleSortOption
import com.example.domain.repository.RoleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * UseCase for retrieving project roles with optional filtering and sorting.
 */
interface GetProjectRolesUseCase {
    /**
     * Retrieves roles for a project with optional filtering and sorting.
     *
     * @param projectId The ID of the project.
     * @param filterIsDefault Optional filter to only include default roles.
     * @param sortBy Optional sort option for the roles.
     * @return A [Flow] emitting a list of [Role] objects.
     */
    operator fun invoke(
        projectId: String,
        filterIsDefault: Boolean? = null,
        sortBy: RoleSortOption? = null
    ): Flow<List<Role>>
}
class GetProjectRolesUseCaseImpl @Inject constructor(
    private val roleRepository: RoleRepository
) : GetProjectRolesUseCase {

    /**
     * Retrieves roles for a project with optional filtering and sorting.
     *
     * @param projectId The ID of the project.
     * @param filterIsDefault Optional filter to only include default roles.
     * @param sortBy Optional sort option for the roles.
     * @return A [Flow] emitting a list of [Role] objects.
     */
    override fun invoke(
        projectId: String,
        filterIsDefault: Boolean?,
        sortBy: RoleSortOption?
    ): Flow<List<Role>> {
        return roleRepository.getProjectRolesStream(projectId).map { customResult ->
            val roles = when (customResult) {
                is CustomResult.Success -> customResult.data
                is CustomResult.Failure -> {
                    // Optionally log customResult.error here
                    emptyList<Role>() // Return empty list on failure
                }
                else -> {
                    Log.e("GetProjectRolesUseCaseImpl", "Unexpected CustomResult type: $customResult")
                    emptyList<Role>() // Return empty list on failure
                }
            }
            var result = roles.toList()

            // Apply filter if specified
            filterIsDefault?.let { isDefault ->
                result = result.filter { it.isDefault == isDefault }
            }

            // Apply sorting if specified
            sortBy?.let { option ->
                result = when (option) {
                    RoleSortOption.NAME_ASC -> result.sortedBy { it.name.lowercase() }
                    RoleSortOption.NAME_DESC -> result.sortedByDescending { it.name.lowercase() }
                    else -> result.sortedBy { it.name.lowercase() }
                }
            }

            result
        }
    }
}
