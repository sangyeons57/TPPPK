package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.role.RoleIsDefault
import com.example.domain.repository.base.ProjectRoleRepository
import javax.inject.Inject
import kotlin.Result

/**
 * UseCase for creating a new project role.
 */
interface CreateProjectRoleUseCase {
    /**
     * Creates a new role within a project.
     *
     * @param name The name of the new role.
     * @param isDefault Whether the new role should be a default role. Defaults to false.
     * @return A [Result] containing the ID of the newly created role, or an error.
     */
    suspend operator fun invoke(
        name: String,
        isDefault: Boolean = false
    ): CustomResult<DocumentId, Exception>
}

/**
 * Implementation of [CreateProjectRoleUseCase].
 */
class CreateProjectRoleUseCaseImpl @Inject constructor(
    private val projectRoleRepository: ProjectRoleRepository
) : CreateProjectRoleUseCase {

    override suspend operator fun invoke(
        name: String,
        isDefault: Boolean
    ): CustomResult<DocumentId, Exception> {
        val role = Role.create(
            name = Name(name),
            isDefault = RoleIsDefault(isDefault)
        )
        return projectRoleRepository.save(role)
    }
}
