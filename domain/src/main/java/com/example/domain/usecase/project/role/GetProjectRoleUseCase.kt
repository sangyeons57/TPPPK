package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.RoleRepository
import javax.inject.Inject
import kotlin.Result

/**
 * UseCase for retrieving a specific project role.
 */
interface GetProjectRoleUseCase {
    /**
     * Retrieves a specific role from a project.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to retrieve.
     * @return A [Result] containing the [Role] if found, or null if not found, or an error.
     */
    suspend operator fun invoke(roleId: String): CustomResult<Role, Exception>
}

/**
 * Implementation of [GetProjectRoleUseCase].
 */
class GetProjectRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: RoleRepository
) : GetProjectRoleUseCase {

    override suspend operator fun invoke(roleId: String): CustomResult<Role, Exception> {
        return when (val result =projectRoleRepository.findById(DocumentId(roleId))){
            is CustomResult.Success -> CustomResult.Success(result.data as Role)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}
