
package com.example.data.datasource.remote

import com.example.data.model.remote.PermissionDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.dataObjects
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PermissionRemoteDataSource {

    companion object {
        private const val PROJECTS_COLLECTION = "projects"
        private const val ROLES_COLLECTION = "roles"
        private const val PERMISSIONS_COLLECTION = "permissions"
    }

    private fun getPermissionsCollection(projectId: String, roleId: String) =
        firestore.collection(PROJECTS_COLLECTION).document(projectId)
            .collection(ROLES_COLLECTION).document(roleId)
            .collection(PERMISSIONS_COLLECTION)

    override fun observePermissions(projectId: String, roleId: String): Flow<List<PermissionDTO>> {
        return getPermissionsCollection(projectId, roleId).snapshots()
            .map{ snapshot -> snapshot.documents.mapNotNull { it.toObject(PermissionDTO::class.java) } }
    }

    override fun observePermission(permissionId: String): Flow<PermissionDTO> {
        TODO("Not yet implemented")
    }

    override suspend fun addPermissionToRole(
        projectId: String,
        roleId: String,
        permission: PermissionDTO
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            // 권한의 이름(예: "CAN_DELETE_MESSAGE")을 문서 ID로 사용하여 중복을 방지합니다.
            getPermissionsCollection(projectId, roleId).document(permission.id).set(permission).await()
            Unit
        }
    }

    override suspend fun removePermissionFromRole(
        projectId: String,
        roleId: String,
        permissionId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            getPermissionsCollection(projectId, roleId).document(permissionId).delete().await()
            Unit
        }
    }
    
    private inline fun <T> resultTry(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Throwable) {
            if (e is java.util.concurrent.CancellationException) throw e
            Result.failure(e)
        }
    }
}

