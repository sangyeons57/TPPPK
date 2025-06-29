package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.PermissionRemoteDataSource
import com.example.data.model.remote.PermissionDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Member
// import com.example.data.datasource.remote.PermissionRemoteDataSource // No longer needed for getAllPermissions, keep if getPermissionById still needs it
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission // Import RolePermission enum
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.permission.PermissionDescription
import com.example.domain.repository.factory.context.PermissionRepositoryFactoryContext
import com.example.domain.repository.base.PermissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
// import kotlinx.coroutines.flow.Flow // Not used in this file anymore
// import kotlinx.coroutines.flow.map // Not used in this file anymore
import javax.inject.Inject

class PermissionRepositoryImpl @Inject constructor(
    private val permissionRemoteDataSource: PermissionRemoteDataSource,
    override val factoryContext: PermissionRepositoryFactoryContext, // Keep if getPermissionById still needs it
    // TODO: 필요한 Mapper 주입
) : DefaultRepositoryImpl(permissionRemoteDataSource, factoryContext), PermissionRepository {

    // Helper function to format enum names (e.g., "MANAGE_PROJECT" -> "Manage Project")
    private fun formatEnumName(enumName: String): String {
        return enumName.split('_').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }


    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Permission)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Permission"))
        ensureCollection()
        return if (entity.isNew) {
            permissionRemoteDataSource.create(entity.toDto())
        } else {
            permissionRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }

}
