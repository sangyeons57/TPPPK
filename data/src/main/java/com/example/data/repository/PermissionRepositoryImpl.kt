package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.PermissionRemoteDataSource
import com.example.data.model.mapper.toDomain // TODO: 실제 매퍼 경로 및 함수 확인
import com.example.domain.model.base.Permission
import com.example.domain.repository.PermissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PermissionRepositoryImpl @Inject constructor(
    private val permissionRemoteDataSource: PermissionRemoteDataSource
    // TODO: 필요한 Mapper 주입
) : PermissionRepository {

    override fun getAllPermissionsStream(): Flow<CustomResult<List<Permission>>> {
        return permissionRemoteDataSource.getAllPermissionsStream().map { result ->
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() } // TODO: PermissionDto를 Permission으로 매핑
            }
        }
    }

    override suspend fun getPermissionById(permissionId: String): CustomResult<Permission> {
        return permissionRemoteDataSource.getPermissionById(permissionId).mapCatching { dto ->
            dto.toDomain() // TODO: PermissionDto를 Permission으로 매핑
        }
    }
}
