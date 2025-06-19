
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.CategoryDTO
import com.example.data.model.remote.ProjectDTO
import com.example.domain.model.vo.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

interface CategoryRemoteDataSource : DefaultDatasource<CategoryDTO> { // DefaultDatasource 상속

    /**
     * 특정 프로젝트의 모든 카테고리 목록을 순서대로 실시간 관찰합니다.
     * @param projectId 카테고리를 가져올 프로젝트의 ID
     */
    fun observeCategories(projectId: String): Flow<CustomResult<List<CategoryDTO>, Exception>>

}


@Singleton
class CategoryRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore // FirebaseAuth 의존성 제거
) : DefaultDatasourceImpl<CategoryDTO>(firestore, CategoryDTO::class.java), CategoryRemoteDataSource {

    /**
     * 특정 프로젝트의 모든 카테고리 목록을 순서대로 실시간 관찰합니다.
     * @param projectId 카테고리를 가져올 프로젝트의 ID.
     * @return List<CategoryDTO>를 방출하는 Flow.
     */
    override fun observeCategories(projectId: String): Flow<CustomResult<List<CategoryDTO>, Exception>> = callbackFlow {
        val categoriesCollection = firestore.collection(ProjectDTO.COLLECTION_NAME)
            .document(projectId)
            .collection(CategoryDTO.COLLECTION_NAME)
            .orderBy(CategoryDTO.ORDER, Query.Direction.ASCENDING) // 'order' 필드로 정렬

        val listenerRegistration = categoriesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(CustomResult.Failure(error))
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val categories = snapshot.toObjects(CategoryDTO::class.java)
                trySend(CustomResult.Success(categories))
            } else {
                // Firestore 스냅샷이 null인 경우는 거의 없지만, 방어적으로 처리
                trySend(CustomResult.Failure(Exception("Failed to observe categories: snapshot is null for project ID $projectId.")))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }
}

