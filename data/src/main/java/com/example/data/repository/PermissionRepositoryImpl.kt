package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
// import com.example.data.datasource.remote.PermissionRemoteDataSource // No longer needed for getAllPermissions, keep if getPermissionById still needs it
import com.example.domain.model.base.Permission
import com.example.domain.model.data.project.RolePermission // Import RolePermission enum
import com.example.domain.repository.PermissionRepository
// import kotlinx.coroutines.flow.Flow // Not used in this file anymore
// import kotlinx.coroutines.flow.map // Not used in this file anymore
import javax.inject.Inject

class PermissionRepositoryImpl @Inject constructor(
    // private val permissionRemoteDataSource: PermissionRemoteDataSource // Keep if getPermissionById still needs it
    // TODO: 필요한 Mapper 주입
) : PermissionRepository {

    // Helper function to format enum names (e.g., "MANAGE_PROJECT" -> "Manage Project")
    private fun formatEnumName(enumName: String): String {
        return enumName.split('_').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }


    override suspend fun getAllPermissions(projectId: String): CustomResult<List<Permission>, Exception> {
        // projectId is no longer strictly necessary here as permissions are globally defined by the enum,
        // but kept for consistency with the interface, or if future project-specific filtering is needed.
        return resultTry {
            val allPermissions = RolePermission.values().map { enumEntry ->
                Permission(
                    id = enumEntry.name, // e.g., "MANAGE_PROJECT"
                    name = formatEnumName(enumEntry.name), // e.g., "Manage Project"
                    description = "Allows ${formatEnumName(enumEntry.name).lowercase()}" // Generic description
                )
            }
            allPermissions // This is already List<Permission>, so CustomResult.Success will be handled by resultTry
        }
    }

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
