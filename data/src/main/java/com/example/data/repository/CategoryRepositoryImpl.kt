package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.model.remote.toDto
import com.example.domain.model.base.Category
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryRemoteDataSource: CategoryRemoteDataSource
) : CategoryRepository {

    /**
     * 새로운 카테고리를 생성합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     */
    override suspend fun createCategory(
        projectId: String,
        category: Category
    ): CustomResult<String, Exception> {
        return categoryRemoteDataSource.addCategory(projectId, category.toDto())
    }

    /**
     * 기존 카테고리를 업데이트합니다.
     * Firebase의 자체 캐싱 시스템을 활용합니다.
     */
    override suspend fun updateCategory(
        projectId: String,
        category: Category
    ): CustomResult<Unit, Exception> {
        return categoryRemoteDataSource.updateCategory(projectId, category.toDto())
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
        return categoryRemoteDataSource.getCategory(projectId, categoryId).mapCatching { dto ->
            dto.toDomain()
        }
    }

    /**
     * 카테고리 목록을 스트림으로 가져옵니다.
     * Firebase의 자체 캐싱 시스템을 활용하여 실시간 업데이트를 처리합니다.
     */
    override suspend fun getCategoriesStream(projectId: String): Flow<CustomResult<List<Category>, Exception>> {
        return categoryRemoteDataSource.observeCategories(projectId).map { dtoList ->
            CustomResult.Success(dtoList.map { it.toDomain() })
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
        return categoryRemoteDataSource.updateCategoriesOrder(
            projectId,
            categories.map { it.toDto() })
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
            // Firebase의 캐싱 시스템을 초기화하여 최신 데이터를 가져오도록 설정된 함수 호출
            val remoteCategoriesResult = categoryRemoteDataSource.getCategories(projectId)
            remoteCategoriesResult.mapCatching { _ -> Unit }
        } catch (e: Exception) {
            CustomResult.Error(e)
        }
    }
}
