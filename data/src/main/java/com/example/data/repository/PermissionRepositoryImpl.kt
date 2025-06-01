package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.PermissionRemoteDataSource
import com.example.domain.model.base.Permission
import com.example.domain.repository.PermissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PermissionRepositoryImpl @Inject constructor(
    private val permissionRemoteDataSource: PermissionRemoteDataSource
    // TODO: 필요한 Mapper 주입
) : PermissionRepository {


    override suspend fun getPermissionById(permissionId: String): CustomResult<Permission, Exception> {
        TODO ("에러 다수정하고 권한 기능 만들떄 작업하기")
        /**
        return when (val result = permissionRemoteDataSource.observePermission(permissionId)) {
            is CustomResult.Success -> {
                try {
                    val permission = result.data.toDomain() // TODO: PermissionDto를 Permission으로 매핑
                    CustomResult.Success(permission)
                } catch (e: Exception) {
                    CustomResult.Failure(e)
                }
            }
            is CustomResult.Failure -> {
                CustomResult.Failure(result.error)
            }
            else -> {
                CustomResult.Failure(Exception("Unknown error in getPermissionById"))
            }
        }
         **/
    }
}
