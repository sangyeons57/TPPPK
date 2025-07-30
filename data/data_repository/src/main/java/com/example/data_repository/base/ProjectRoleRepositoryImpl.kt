package com.example.data_repository.base

import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.RoleRemoteDataSource
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Role
import com.example.domain.model.data.project.RolePermission
import com.example.domain.model.vo.DocumentId
import com.example.domain_repository.base.ProjectRoleRepository
import com.example.mapper.role.RoleMapper
import javax.inject.Inject

class ProjectRoleRepositoryImpl @Inject constructor(
    private val roleRemoteDataSource: RoleRemoteDataSource,
    private val roleMapper: RoleMapper,
) : DefaultRepositoryImpl(roleRemoteDataSource), ProjectRoleRepository {

    override suspend fun getRolePermissions(
        projectId: String,
        roleId: String
    ): CustomResult<List<RolePermission>, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Role)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Role"))
        ensureCollection()
        return if (entity.isNew) {
            roleRemoteDataSource.create(roleMapper.domainToDto(entity))
        } else {
            roleRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
