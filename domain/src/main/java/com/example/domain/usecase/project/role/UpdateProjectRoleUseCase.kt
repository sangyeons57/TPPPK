package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.Permission
import com.example.domain.model.base.Role
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.role.RoleIsDefault
import com.example.domain.repository.base.RoleRepository
import javax.inject.Inject
import kotlin.Result

/**
 * UseCase for updating a project role.
 */
interface UpdateProjectRoleUseCase {
    /**
     * Updates an existing role within a project.
     *
     * @param projectId The ID of the project.
     * @param roleId The ID of the role to update.
     * @param name Optional. The new name for the role. If null, the name is not changed.
     * @param permissions Optional. The new permissions map for the role. If null, permissions are not changed.
     * @param isDefault Optional. The new default status for the role. If null, the default status is not changed.
     * @return A [Result] indicating success or failure.
     */
    suspend operator fun invoke(
        roleId: String,
        name: String? = null,
        isDefault: Boolean? = null
    ): CustomResult<Unit, Exception>
}
/**
 * Implementation of [UpdateProjectRoleUseCase].
 */
class UpdateProjectRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: RoleRepository
) : UpdateProjectRoleUseCase {

    override suspend operator fun invoke(
        roleId: String,
        name: String?,
        isDefault: Boolean?
    ): CustomResult<Unit, Exception> {
        // Fetch the current role details to get existing values if not provided
        val currentRole = when (val result = projectRoleRepository.findById(DocumentId(roleId))) {
            is CustomResult.Success -> result.data as Role
            is CustomResult.Failure -> return CustomResult.Failure(result.error)
            is CustomResult.Initial -> return CustomResult.Initial
            is CustomResult.Loading -> return CustomResult.Loading
            is CustomResult.Progress -> return CustomResult.Progress(result.progress)
        }
        name?.let {currentRole.changeName(Name(it))}
        isDefault?.let {currentRole.setDefault(RoleIsDefault(it))}

        return when (val result = projectRoleRepository.save(currentRole)) {
            is CustomResult.Success -> {
                EventDispatcher.publish(currentRole)
                CustomResult.Success(Unit)
            }
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}
