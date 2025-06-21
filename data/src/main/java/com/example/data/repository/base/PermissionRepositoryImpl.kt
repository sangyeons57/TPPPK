package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.PermissionRemoteDataSource
import com.example.data.model.remote.PermissionDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.event.AggregateRoot
// import com.example.data.datasource.remote.PermissionRemoteDataSource // No longer needed for getAllPermissions, keep if getPermissionById still needs it
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission // Import RolePermission enum
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.permission.PermissionDescription
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.base.PermissionRepository
// import kotlinx.coroutines.flow.Flow // Not used in this file anymore
// import kotlinx.coroutines.flow.map // Not used in this file anymore
import javax.inject.Inject

class PermissionRepositoryImpl @Inject constructor(
    private val permissionRemoteDataSource: PermissionRemoteDataSource,
    override val factoryContext: DefaultRepositoryFactoryContext, // Keep if getPermissionById still needs it
    // TODO: 필요한 Mapper 주입
) : DefaultRepositoryImpl(permissionRemoteDataSource, factoryContext.collectionPath), PermissionRepository {

    // Helper function to format enum names (e.g., "MANAGE_PROJECT" -> "Manage Project")
    private fun formatEnumName(enumName: String): String {
        return enumName.split('_').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }


    override suspend fun getAllPermissions(projectId: String): CustomResult<List<Permission>, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Permission)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Permission"))

        return if (entity.id.isAssigned()) {
            permissionRemoteDataSource.update(entity.id, entity.getChangedFields())
        } else {
            permissionRemoteDataSource.create(entity.toDto())
        }
    }

}
