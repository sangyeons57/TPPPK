
package com.example.data.datasource.remote

import com.example.data.model._remote.RoleDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.dataObjects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
    }

    private fun getRolesCollection(projectId: String) =
        firestore.collection(PROJECTS_COLLECTION).document(projectId)
            .collection(ROLES_COLLECTION)

    override fun observeRoles(projectId: String): Flow<List<RoleDTO>> {
        return getRolesCollection(projectId).dataObjects()
    }

    override suspend fun addRole(projectId: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        resultTry {
            val newRole = RoleDTO(
                name = name,
                isDefault = false // 사용자가 추가하는 역할은 항상 커스텀 역할
            )
            val documentReference = getRolesCollection(projectId).add(newRole).await()
            documentReference.id
        }
    }

    override suspend fun updateRole(
        projectId: String,
        roleId: String,
        newName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            // 참고: isDefault가 true인 역할은 수정할 수 없도록 하는 로직은
            // UseCase나 Repository 계층에서 처리하는 것이 좋습니다.
            getRolesCollection(projectId).document(roleId)
                .update("name", newName).await()
            Unit
        }
    }

    override suspend fun deleteRole(projectId: String, roleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            // 참고: isDefault가 true인 역할은 삭제할 수 없도록 하는 로직과,
            // 이 역할을 가진 멤버가 있는지 확인하는 로직 등은
            // UseCase나 Repository 계층에서 처리하는 것이 좋습니다.
            getRolesCollection(projectId).document(roleId)
                .delete().await()
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

