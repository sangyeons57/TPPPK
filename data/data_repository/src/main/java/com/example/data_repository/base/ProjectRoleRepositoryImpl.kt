package com.example.data_repository.base

import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.PermissionRemoteDataSource
import com.example.data_datasource.remote.RoleRemoteDataSource
import com.example.data_model.remote.PermissionDTO
import com.example.data_model.remote.RoleDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.base.Role
import com.example.domain.model.data.project.RolePermission
import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.ProjectRoleRepository
import com.example.mapper.DtoMapper
import javax.inject.Inject

class ProjectRoleRepositoryImpl @Inject constructor(
    roleRemoteDataSource: RoleRemoteDataSource,
    private val roleMapper: DtoMapper<Role, RoleDTO>,
    private val permissionRemoteDataSource: PermissionRemoteDataSource,
) : DefaultRepositoryImpl<Role, RoleDTO>(roleRemoteDataSource, roleMapper), ProjectRoleRepository {

    override suspend fun getRolePermissions(
        projectId: String,
        roleId: String
    ): CustomResult<List<RolePermission>, Exception> {
        // Scope the permission datasource to the role's permissions subcollection
        val path = CollectionPath.projectRolePermissions(projectId, roleId)
        permissionRemoteDataSource.setCollection(path)

        return when (val result = permissionRemoteDataSource.findAll()) {
            is CustomResult.Success -> {
                try {
                    val perms = result.data.mapNotNull { dto ->
                        // Map DTO id to RolePermission enum
                        RolePermission.from((dto as PermissionDTO).id)
                    }
                    CustomResult.Success(perms)
                } catch (e: Exception) {
                    CustomResult.Failure(e)
                }
            }

            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }
}
