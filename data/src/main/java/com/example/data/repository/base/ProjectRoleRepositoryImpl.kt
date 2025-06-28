package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.RoleRemoteDataSource
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Role
import com.example.domain.model.data.project.RolePermission
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.base.ProjectRoleRepository
import javax.inject.Inject

class ProjectRoleRepositoryImpl @Inject constructor(
    private val roleRemoteDataSource: RoleRemoteDataSource,
    override val factoryContext: DefaultRepositoryFactoryContext
    // private val roleMapper: RoleMapper // 개별 매퍼 사용시
) : DefaultRepositoryImpl(roleRemoteDataSource, factoryContext.collectionPath), ProjectRoleRepository {

    override suspend fun getRolePermissions(
        projectId: String,
        roleId: String
    ): CustomResult<List<RolePermission>, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Role)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Role"))

        return if (entity.id.isAssigned()) {
            roleRemoteDataSource.update(entity.id, entity.getChangedFields())
        } else {
            roleRemoteDataSource.create(entity.toDto())
        }
    }

    override suspend fun create(id: DocumentId, entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Role)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Role"))
        return roleRemoteDataSource.create(entity.toDto())
    }
}
