
package com.example.data.datasource.remote

import com.example.data.model.remote.CategoryDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.dataObjects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CategoryRemoteDataSource {

    companion object {
        private const val PROJECTS_COLLECTION = "projects"
        private const val CATEGORIES_COLLECTION = "categories"
    }

    private fun getCategoriesCollection(projectId: String) =
        firestore.collection(PROJECTS_COLLECTION).document(projectId)
            .collection(CATEGORIES_COLLECTION)

    override fun observeCategories(projectId: String): Flow<List<CategoryDTO>> {
        return getCategoriesCollection(projectId)
            .orderBy("order", Query.Direction.ASCENDING)
            .dataObjects()
    }

    override suspend fun addCategory(
        projectId: String,
        name: String,
        order: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            val newCategory = CategoryDTO(
                name = name,
                order = order,
                createdBy = uid
            )
            val docRef = getCategoriesCollection(projectId).add(newCategory).await()
            docRef.id
        }
    }

    override suspend fun updateCategory(
        projectId: String,
        categoryId: String,
        newName: String,
        newOrder: Double
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val updateData = mapOf(
                "name" to newName,
                "order" to newOrder,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            getCategoriesCollection(projectId).document(categoryId).update(updateData).await()
            Unit
        }
    }

    override suspend fun deleteCategory(
        projectId: String,
        categoryId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            // 경고: 이 작업은 카테고리 문서만 삭제합니다.
            // 이 카테고리 안에 있던 채널(ProjectChannel)들은 삭제되지 않으므로,
            // 실제 앱에서는 관련 채널들을 먼저 다른 카테고리로 옮기거나 삭제하는 로직이 필요합니다.
            getCategoriesCollection(projectId).document(categoryId).delete().await()
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

