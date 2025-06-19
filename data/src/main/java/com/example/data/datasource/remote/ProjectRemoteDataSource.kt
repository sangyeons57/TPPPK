
package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.data.model.remote.ProjectDTO
import kotlinx.coroutines.tasks.await

interface ProjectRemoteDataSource : DefaultDatasource<ProjectDTO> {

    /**
     * Firestore에서 특정 프로젝트의 프로필 이미지 URL 필드만 업데이트합니다.
     *
     * @param projectId 업데이트할 프로젝트의 ID.
     * @param imageUrl 새 프로필 이미지의 다운로드 URL. null일 경우 필드를 제거하거나 기본값으로 설정할 수 있습니다.
     * @return 작업 성공 시 [CustomResult.Success] (Unit), 실패 시 [CustomResult.Failure] (Exception).
     */
    suspend fun updateProjectProfileImageUrl(projectId: String, imageUrl: String?): CustomResult<Unit, Exception>
}

@Singleton
class ProjectRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<ProjectDTO>(firestore, ProjectDTO::class.java),
    ProjectRemoteDataSource {

    override suspend fun updateProjectProfileImageUrl(projectId: String, imageUrl: String?): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        checkCollectionInitialized("updateProjectProfileImageUrl")
        if (projectId.isBlank()) return@withContext CustomResult.Failure(IllegalArgumentException("Project ID cannot be blank"))
        resultTry {
            val data = hashMapOf(
                ProjectDTO.IMAGE_URL to imageUrl,
                ProjectDTO.UPDATED_AT to FieldValue.serverTimestamp()
            )
            collection.document(projectId).update(data).await()
            Unit
        }
    }
}

