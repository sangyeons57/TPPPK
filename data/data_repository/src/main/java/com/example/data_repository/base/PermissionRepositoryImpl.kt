package com.example.data_repository.base

import com.example.data_datasource.remote.PermissionRemoteDataSource
import com.example.data_model.remote.PermissionDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.base.Permission
import com.example.domain_repository.base.PermissionRepository
import com.example.mapper.DtoMapper
import javax.inject.Inject

class PermissionRepositoryImpl @Inject constructor(
    permissionRemoteDataSource: PermissionRemoteDataSource,
    private val permissionMapper: DtoMapper<Permission, PermissionDTO>,
) : DefaultRepositoryImpl<Permission, PermissionDTO>(permissionRemoteDataSource, permissionMapper),
    PermissionRepository {

    // Helper function to format enum names (e.g., "MANAGE_PROJECT" -> "Manage Project")
    private fun formatEnumName(enumName: String): String {
        return enumName.split('_').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

}
