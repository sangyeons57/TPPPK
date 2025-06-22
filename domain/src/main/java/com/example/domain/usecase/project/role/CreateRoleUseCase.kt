package com.example.domain.usecase.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Role
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.role.RoleIsDefault
import com.example.domain.repository.base.RoleRepository
import javax.inject.Inject

interface CreateRoleUseCase {
    suspend operator fun invoke(roleName: String, isDefault: Boolean): CustomResult<DocumentId, Exception> // Return Role ID string
}

class CreateRoleUseCaseImpl @Inject constructor(
    private val roleRepository: RoleRepository
) : CreateRoleUseCase {

    override suspend operator fun invoke(
        roleName: String,
        isDefault: Boolean
    ): CustomResult<DocumentId, Exception> {
        val role = Role.create(
            name = Name(roleName),
            isDefault = RoleIsDefault(isDefault)
        )
        return roleRepository.save(role)
    }
}
