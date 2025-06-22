package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.PermissionRemoteDataSource // 권한 목록 조회 시 필요할 수 있음
import com.example.data.datasource.remote.RoleRemoteDataSource
import com.example.data.model.remote.RoleDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.event.AggregateRoot
import com.example.domain.model.base.Role
import com.example.domain.model.data.project.RolePermission
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.base.RoleRepository
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoleRepositoryImpl @Inject constructor(
    private val roleRemoteDataSource: RoleRemoteDataSource,
    override val factoryContext: DefaultRepositoryFactoryContext
    // private val roleMapper: RoleMapper // 개별 매퍼 사용시
) : DefaultRepositoryImpl(roleRemoteDataSource, factoryContext.collectionPath), RoleRepository {

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
