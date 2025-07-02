package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import com.example.domain.model.ui.project.RoleSortOption
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectRoleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
    suspend operator fun invoke(
        projectId: DocumentId,
        sortBy: RoleSortOption? = null
    ): Flow<CustomResult<List<Role>, Exception>>
}
class GetProjectRolesUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository
) : GetProjectRolesUseCase {

    override suspend fun invoke(
        projectId: DocumentId,
        sortBy: RoleSortOption?
    ): Flow<CustomResult<List<Role>, Exception>> {
        return when (val customResult = projectRoleRepository.observeAll().first()){
            is CustomResult.Success -> {
                // Filter to only include Role objects and remove the default filter 
                // so we can see both default and custom roles
                var roles = customResult.data.filterIsInstance<Role>()

                // Apply sorting if specified
                sortBy?.let { option ->
                    roles = when (option) {
                        RoleSortOption.NAME_ASC -> roles.sortedBy { it.name.lowercase() }
                        RoleSortOption.NAME_DESC -> roles.sortedByDescending { it.name.lowercase() }
                        else -> roles
                    }
                }

                flowOf(CustomResult.Success(roles))
            }
            is CustomResult.Failure -> flowOf(CustomResult.Failure(customResult.error))
            is CustomResult.Initial -> flowOf(CustomResult.Initial)
            is CustomResult.Loading -> flowOf(CustomResult.Loading)
            is CustomResult.Progress -> flowOf(CustomResult.Progress(customResult.progress))
        }
    }
}
