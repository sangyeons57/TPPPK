
package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.data.model.remote.ProjectDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
class ProjectRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ProjectRemoteDataSource {

    // FirestoreConstants에서 정의된 상수 사용

    private val projectsCollection = firestore.collection(FirestoreConstants.Collections.PROJECTS)

    override fun observeProject(projectId: String): Flow<ProjectDTO?> {
        return projectsCollection.document(projectId).snapshots()
            .map { snapshot -> snapshot.toObject(ProjectDTO::class.java) }
    }

    override suspend fun getProject(projectId: String): CustomResult<ProjectDTO, Exception> = withContext(Dispatchers.IO) {
        resultTry<ProjectDTO> {
            if (projectId.isBlank()) {
                throw Exception("Invalid project ID.")
            }
            val document = projectsCollection.document(projectId).get().await()
            return@resultTry document.toObject<ProjectDTO>(ProjectDTO::class.java) ?: throw Exception("Project not found.")
        }
    }

    override suspend fun createProject(projectDTO: ProjectDTO): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            
            // Ensure ownerId is set from auth, potentially overriding DTO if necessary,
            // or validate that DTO's ownerId matches current user if it's pre-set.
            // For now, assuming DTO might not have ownerId or it should be overwritten by current user.
            val newProject = projectDTO.copy(
                ownerId = uid
                // createdAt, updatedAt은 DTO에서 @ServerTimestamp로 자동 설정됩니다.
            )

            val documentReference = projectsCollection.add(newProject).await()
            documentReference.id // 성공 시 새 문서의 ID를 반환
        }
    }

    override suspend fun updateProjectDetails(
        projectId: String,
        projectDTO: ProjectDTO
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val updateData = mutableMapOf<String, Any?>()
            updateData["name"] = projectDTO.name
            updateData["imageUrl"] = projectDTO.imageUrl // Assuming imageUrl is part of ProjectDTO
            updateData["updatedAt"] = FieldValue.serverTimestamp()
            // Consider other fields from projectDTO that might need updating.
            // For now, only name and imageUrl as per original logic.

            projectsCollection.document(projectId).update(updateData).await()
            Unit // 성공 시 Unit 반환
        }
    }

    override suspend fun deleteProject(projectId: String): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            // 경고: 이 작업은 프로젝트 문서 자체만 삭제합니다.
            // 하위 컬렉션(members, roles 등)은 삭제되지 않으므로,
            // 프로덕션 환경에서는 Cloud Functions를 사용하여 모든 하위 데이터를 함께 삭제하는 것이 안전합니다.
            projectsCollection.document(projectId).delete().await()
            Unit
        }
    }

    // 사용자님이 제공해주신 코드와 동일한 역할을 하는 헬퍼 함수
    private inline fun <T> resultTry(block: () -> T): CustomResult<T, Exception> {
        return try {
            CustomResult.Success(block())
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}

