package com.example.data_repository.base

// import com.example.data.datasource.remote.PermissionRemoteDataSource // No longer needed for getAllPermissions, keep if getPermissionById still needs it
// import kotlinx.coroutines.flow.Flow // Not used in this file anymore
// import kotlinx.coroutines.flow.map // Not used in this file anymore
import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.PermissionRemoteDataSource
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Permission
import com.example.domain.model.vo.DocumentId
import com.example.domain_repository.base.PermissionRepository
import com.example.mapper.permission.PermissionMapper
import javax.inject.Inject

class PermissionRepositoryImpl @Inject constructor(
    private val permissionRemoteDataSource: PermissionRemoteDataSource,
    private val permissionMapper: PermissionMapper,
) : DefaultRepositoryImpl(permissionRemoteDataSource), PermissionRepository {

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
            permissionRemoteDataSource.create(permissionMapper.domainToDto(entity))
        } else {
            permissionRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }

}
