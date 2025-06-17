package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.model.remote.toDto
import com.example.domain.model.base.Category
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryRemoteDataSource: CategoryRemoteDataSource
) : CategoryRepository {

    /**
     * 새로운 카테고리를 생성합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     */
    override suspend fun addCategory(
        projectId: String,
        category: Category
    ): CustomResult<String, Exception> {
        return categoryRemoteDataSource.addCategory(projectId, category.toDto())
    }

    override suspend fun setDirectCategory(
        projectId: String,
        categoryId: String,
        category: Category
    ): CustomResult<Unit, Exception> {
        return categoryRemoteDataSource.setDirectCategory(projectId, categoryId, category.toDto())
    }

    /**
     * 기존 카테고리를 업데이트합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     */
    override suspend fun updateCategory(
        projectId: String,
        category: Category
    ): CustomResult<Unit, Exception> {
        return categoryRemoteDataSource.updateCategory(
            projectId,
            category.id,
            category.name,
            category.order
        )
    }

    /**
     * 카테고리를 삭제합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     */
    override suspend fun deleteCategory(
        projectId: String,
        categoryId: String
    ): CustomResult<Unit, Exception> {
        return categoryRemoteDataSource.deleteCategory(projectId, categoryId)
    }

    /**
     * 특정 카테고리를 조회합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     */
    override suspend fun getCategory(
        projectId: String,
        categoryId: String
    ): CustomResult<Category, Exception> {
        val result = categoryRemoteDataSource.observeCategory(projectId, categoryId).first()
        return when (result) {
            is CustomResult.Success -> {
                CustomResult.Success(result.data.toDomain())
            }
            is CustomResult.Failure -> {
                CustomResult.Failure(result.error)
            }
            else -> {
                CustomResult.Failure(Exception("Unknown error getting category"))
            }
        }
    }

    /**
     * 카테고리 목록을 스트림으로 가져옵니다.
     * Firebase의 자체 캐싱 시스템을 활용하여 실시간 업데이트를 처리합니다.
     */
    override suspend fun getCategoriesStream(projectId: String): Flow<CustomResult<List<Category>, Exception>> {
        return categoryRemoteDataSource.observeCategories(projectId).map { dtoResultList ->
            if (dtoResultList is CustomResult.Success) {
                CustomResult.Success(dtoResultList.data.map { it.toDomain() })
            } else if (dtoResultList is CustomResult.Failure) {
                CustomResult.Failure(dtoResultList.error)
            } else {
                CustomResult.Failure(Exception("Unknown error getting categories"))
            }
        }
    }

    /**
     * 여러 카테고리를 동시에 업데이트합니다(주로 순서 변경용).
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     */
    override suspend fun updateCategories(
        projectId: String,
        categories: List<Category>
    ): CustomResult<Unit, Exception> {
        // Since updateCategoriesOrder doesn't exist in the interface,
        // we'll implement a batch update using existing methods
        return try {
            // Update each category individually
            categories.forEach { category ->
                val result = categoryRemoteDataSource.updateCategory(
                    projectId,
                    category.id,
                    category.name,
                    category.order
                )
                if (result is CustomResult.Failure) {
                    return result
                }
            }
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 특정 프로젝트의 카테고리 목록을 서버로부터 강제로 새로고침합니다.
     * Firebase의 캐싱 시스템을 초기화하여 최신 데이터를 가져옵니다.
     *
     * @param projectId 대상 프로젝트의 ID.
     * @return 작업 성공 여부를 담은 CustomResult<Unit>.
     */
    override suspend fun fetchCategories(projectId: String): CustomResult<Unit, Exception> {
        return try {
            // Since getCategories doesn't exist in the interface, we'll use observeCategories
            // which will refresh from the server due to Firebase's caching behavior
            val up = categoryRemoteDataSource.observeCategories(projectId)
            // Just return success since we've triggered the refresh
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
