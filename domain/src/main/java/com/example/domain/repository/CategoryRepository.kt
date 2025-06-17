package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 내 카테고리 관련 데이터 처리를 위한 인터페이스입니다.
 * 카테고리는 특정 프로젝트에 종속됩니다.
 */
interface CategoryRepository {
    suspend fun addCategory(projectId: String, category: Category): CustomResult<String, Exception>
    suspend fun setDirectCategory(projectId: String, categoryId: String, category: Category): CustomResult<Unit, Exception>
    suspend fun updateCategory(projectId: String, category: Category): CustomResult<Unit, Exception>
    suspend fun deleteCategory(projectId: String, categoryId: String): CustomResult<Unit, Exception>
    suspend fun getCategory(projectId: String, categoryId: String): CustomResult<Category, Exception> // 이전 단계에서 추가됨 (또는 getCategoryById)
    suspend fun getCategoriesStream(projectId: String): Flow<CustomResult<List<Category>, Exception>>
    suspend fun updateCategories(projectId: String, categories: List<Category>): CustomResult<Unit, Exception> // 순서 변경용

    // --- 추가된 메서드 ---
    /**
     * 특정 프로젝트의 카테고리 목록을 서버로부터 강제로 새로고침합니다.
     * 이 함수는 로컬 캐시를 사용하는 경우 최신 데이터를 동기화하거나,
     * 사용자의 명시적인 새로고침 액션에 대응하기 위해 사용될 수 있습니다.
     *
     * @param projectId 대상 프로젝트의 ID.
     * @return 작업 성공 여부를 담은 CustomResult<Unit>.
     */
    suspend fun fetchCategories(projectId: String): CustomResult<Unit, Exception>
}
