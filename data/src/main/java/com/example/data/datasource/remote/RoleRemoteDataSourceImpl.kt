
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
    }

    private fun getRolesCollection(projectId: String) =
        firestore.collection(PROJECTS_COLLECTION).document(projectId)
            .collection(ROLES_COLLECTION)

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
        newName: String
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            // 참고: isDefault가 true인 역할은 수정할 수 없도록 하는 로직은
            // UseCase나 Repository 계층에서 처리하는 것이 좋습니다.
            getRolesCollection(projectId).document(roleId)
                .update("name", newName).await()
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
}

