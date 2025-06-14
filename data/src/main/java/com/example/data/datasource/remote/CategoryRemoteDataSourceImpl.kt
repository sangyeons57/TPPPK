
package com.example.data.datasource.remote

import android.util.Log
import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.model.remote.CategoryDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.dataObjects
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CategoryRemoteDataSource {

    private fun getCategoriesCollection(projectId: String) =
        firestore.collection(FirestoreConstants.Collections.PROJECTS).document(projectId)
            .collection(FirestoreConstants.Project.Categories.COLLECTION_NAME)

    override fun observeCategories(projectId: String): Flow<CustomResult<List<CategoryDTO>, Exception>> {
        return getCategoriesCollection(projectId)
            .orderBy(FirestoreConstants.Project.Categories.ORDER, Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot -> CustomResult.Success(snapshot.toObjects(CategoryDTO::class.java)) }
            .catch { error -> CustomResult.Failure(Exception(error)) }
    }

    override fun observeCategory(
        projectId: String,
        categoryId: String
    ): Flow<CustomResult<CategoryDTO, Exception>> {
        return getCategoriesCollection(projectId).document(categoryId)
            .snapshots()
            .map { snapshot ->
                val category = snapshot.toObject(CategoryDTO::class.java)
                if (category != null) {
                    CustomResult.Success(category)
                } else {
                    CustomResult.Failure(Exception("Category with id $categoryId not found."))
                }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(CustomResult.Failure(Exception(e)))
            }
    }

    override suspend fun addCategory(
        projectId: String,
        categoryDTO: CategoryDTO
    ): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            Log.d("CategoryRemoteDataSourceImpl", "Adding category: $categoryDTO")
            val docRef = getCategoriesCollection(projectId).add(categoryDTO).await()
            docRef.id
        }
    }

    override suspend fun setDirectCategory(
        projectId: String,
        categoryDTO: CategoryDTO
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val docRef = getCategoriesCollection(projectId).document().set(categoryDTO).await()
        }
    }

    override suspend fun updateCategory(
        projectId: String,
        categoryId: String,
        newName: String,
        newOrder: Double
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val updateData = mapOf(
                FirestoreConstants.Project.Categories.NAME to newName,
                FirestoreConstants.Project.Categories.ORDER to newOrder,
                FirestoreConstants.Project.Categories.UPDATED_AT to FieldValue.serverTimestamp()
            )
            getCategoriesCollection(projectId).document(categoryId).update(updateData).await()
            Unit
        }
    }

    override suspend fun deleteCategory(
        projectId: String,
        categoryId: String
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            // 경고: 이 작업은 카테고리 문서만 삭제합니다.
            // 이 카테고리 안에 있던 채널(ProjectChannel)들은 삭제되지 않으므로,
            // 실제 앱에서는 관련 채널들을 먼저 다른 카테고리로 옮기거나 삭제하는 로직이 필요합니다.
            getCategoriesCollection(projectId).document(categoryId).delete().await()
            Unit
        }
    }
}

