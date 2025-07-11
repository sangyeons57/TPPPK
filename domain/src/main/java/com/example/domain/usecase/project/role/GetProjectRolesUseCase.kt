package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import com.example.domain.model.ui.project.RoleSortOption
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectRoleRepository
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
        return projectRoleRepository.observeAll().map { customResult ->
            when (customResult) {
                is CustomResult.Success -> {
                    // 🚨 모든 역할을 보여주되, 시스템 역할(OWNER 등)만 제외
                    // filterIsInstance를 사용하여 타입 안전성 확보 + 시스템 역할 필터링
                    var roles = customResult.data.filterIsInstance<Role>().filter { role ->
                        !Role.isSystemRole(role.id.value)
                    }

                    // Apply sorting if specified
                    sortBy?.let { option ->
                        roles = when (option) {
                            RoleSortOption.NAME_ASC -> roles.sortedBy { it.name.lowercase() }
                            RoleSortOption.NAME_DESC -> roles.sortedByDescending { it.name.lowercase() }
                            else -> roles
                        }
                    }

                    CustomResult.Success(roles)
                }
                is CustomResult.Failure -> CustomResult.Failure(customResult.error)
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Progress -> CustomResult.Progress(customResult.progress)
            }
        }
    }
}
