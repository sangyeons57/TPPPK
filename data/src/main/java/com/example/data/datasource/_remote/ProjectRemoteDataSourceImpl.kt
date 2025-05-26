
package com.example.data.datasource._remote

import com.example.data.model._remote.ProjectDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.dataObjects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ProjectRemoteDataSource {

    companion object {
        private const val PROJECTS_COLLECTION = "projects"
    }

    private val projectsCollection = firestore.collection(PROJECTS_COLLECTION)

    override fun observeProject(projectId: String): Flow<ProjectDTO?> {
        return projectsCollection.document(projectId).dataObjects()
    }

    override suspend fun createProject(name: String, isPublic: Boolean): Result<String> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            
            val newProject = ProjectDTO(
                name = name,
                isPublic = isPublic,
                ownerId = uid
            )

            val documentReference = projectsCollection.add(newProject).await()
            documentReference.id // 성공 시 새 문서의 ID를 반환
        }
    }

    override suspend fun updateProjectDetails(
        projectId: String,
        name: String,
        imageUrl: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val updateData = mapOf(
                "name" to name,
                "imageUrl" to imageUrl,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            projectsCollection.document(projectId).update(updateData).await()
            Unit // 성공 시 Unit 반환
        }
    }

    override suspend fun deleteProject(projectId: String): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            // 경고: 이 작업은 프로젝트 문서 자체만 삭제합니다.
            // 하위 컬렉션(members, roles 등)은 삭제되지 않으므로,
            // 프로덕션 환경에서는 Cloud Functions를 사용하여 모든 하위 데이터를 함께 삭제하는 것이 안전합니다.
            projectsCollection.document(projectId).delete().await()
            Unit
        }
    }

    // 사용자님이 제공해주신 코드와 동일한 역할을 하는 헬퍼 함수
    private inline fun <T> resultTry(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Throwable) {
            if (e is java.util.concurrent.CancellationException) throw e
            Result.failure(e)
        }
    }
}

