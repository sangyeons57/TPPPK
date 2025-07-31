package com.example.data_repository.base

import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.RoleRemoteDataSource
import com.example.data_model.remote.RoleDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.base.Role
import com.example.domain.model.data.project.RolePermission
import com.example.domain_repository.base.ProjectRoleRepository
import com.example.mapper.DtoMapper
import javax.inject.Inject

class ProjectRoleRepositoryImpl @Inject constructor(
    roleRemoteDataSource: RoleRemoteDataSource,
    private val roleMapper: DtoMapper<Role, RoleDTO>,
) : DefaultRepositoryImpl<Role, RoleDTO>(roleRemoteDataSource, roleMapper), ProjectRoleRepository {

    override suspend fun getRolePermissions(
        projectId: String,
        roleId: String
    ): CustomResult<List<RolePermission>, Exception> {
        TODO("Not yet implemented")
    }
}
