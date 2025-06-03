
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.model.remote.RoleDTO
import com.example.domain.model.base.Role
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.dataObjects
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoleRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : RoleRemoteDataSource {

    companion object {
        private const val PROJECTS_COLLECTION = "projects"
        private const val ROLES_COLLECTION = "roles"
        private const val PERMISSIONS_SUB_COLLECTION = "permissions" // New for role-specific permissions
    }

    private fun getRolesCollection(projectId: String) =
        firestore.collection(PROJECTS_COLLECTION).document(projectId)
            .collection(ROLES_COLLECTION)

    // New helper for role's permissions subcollection
    private fun getRolePermissionsSubCollectionRef(projectId: String, roleId: String) =
        firestore.collection(PROJECTS_COLLECTION).document(projectId)
            .collection(ROLES_COLLECTION).document(roleId)
            .collection(PERMISSIONS_SUB_COLLECTION)

    override fun observeRoles(projectId: String): Flow<CustomResult<List<RoleDTO>, Exception>> {
        return getRolesCollection(projectId).snapshots()
            .map { snapshot ->
                resultTry {
                    snapshot.documents.mapNotNull {
                        it.toObject(RoleDTO::class.java)
                    }
                }
            }
    }

    override fun observeRole(projectId: String, roleId: String): Flow<CustomResult<RoleDTO, Exception>> {
        return getRolesCollection(projectId).document(roleId).snapshots()
            .map{
                resultTry {
                    it.toObject<RoleDTO>(RoleDTO::class.java)
                } as CustomResult<RoleDTO, Exception>
            }
    }

    override suspend fun addRole(projectId: String, roleDTO: RoleDTO): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val documentReference = getRolesCollection(projectId).add(roleDTO).await()
            documentReference.id
        }
    }

    override suspend fun updateRole(
        projectId: String,
        roleId: String,
        updates: Map<String, Any?> // Changed from newName: String
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            // Filter out null values from the updates map, as Firestore's update
            // method might not handle nulls as expected for field removal
            // or might cause issues if a field is intended to be explicitly set to null
            // (though for 'name' and 'isDefault', null usually means "no change").
            // For this specific case, if a value is null in the map, we assume it means
            // "do not update this field". If a field should be DELETED,
            // FieldValue.delete() should be used, which is not handled here.
            val nonNullUpdates = updates.filterValues { it != null }

            if (nonNullUpdates.isNotEmpty()) {
                getRolesCollection(projectId).document(roleId)
                    .update(nonNullUpdates).await() // Use the nonNullUpdates map
            }
            Unit
        }
    }

    override suspend fun deleteRole(projectId: String, roleId: String): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            // 참고: isDefault가 true인 역할은 삭제할 수 없도록 하는 로직과,
            // 이 역할을 가진 멤버가 있는지 확인하는 로직 등은
            // UseCase나 Repository 계층에서 처리하는 것이 좋습니다.
            getRolesCollection(projectId).document(roleId)
                .delete().await()
            Unit
        }
    }

    override suspend fun getRolePermissionNames(projectId: String, roleId: String): CustomResult<List<String>, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val snapshot = getRolePermissionsSubCollectionRef(projectId, roleId)
                .get()
                .await()
            // Assuming the document ID itself is the permission name string
            snapshot.documents.map { it.id }
        }
    }

    override suspend fun setRolePermissions(projectId: String, roleId: String, permissionNames: List<String>): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val batch = firestore.batch()
            val permissionsSubCollectionRef = getRolePermissionsSubCollectionRef(projectId, roleId)

            // 1. Delete all existing permissions in the subcollection for this role
            val existingPermissionsSnapshot = permissionsSubCollectionRef.get().await()
            for (document in existingPermissionsSnapshot.documents) {
                batch.delete(document.reference)
            }

            // 2. Add new permissions
            for (permissionName in permissionNames) {
                // Using permissionName as document ID. Document can be empty or have a simple field.
                val newPermissionDocRef = permissionsSubCollectionRef.document(permissionName)
                batch.set(newPermissionDocRef, mapOf("granted" to true)) // Example: storing a simple field
            }

            batch.commit().await()
            Unit
        }
    }
}

