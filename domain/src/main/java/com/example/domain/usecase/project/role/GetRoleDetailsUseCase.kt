package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectRoleRepository
import javax.inject.Inject

interface GetRoleDetailsUseCase {
    suspend operator fun invoke(projectId: String, roleId: String): CustomResult<Role, Exception> // Return type changed to Role?
}

class GetRoleDetailsUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository
) : GetRoleDetailsUseCase {
    override suspend operator fun invoke(projectId: String, roleId: String): CustomResult<Role, Exception> { // Return type changed to Role?
        return when (val result = projectRoleRepository.findById(DocumentId(roleId))) {
            is CustomResult.Success -> CustomResult.Success(result.data as Role) // Cast to Role
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}
