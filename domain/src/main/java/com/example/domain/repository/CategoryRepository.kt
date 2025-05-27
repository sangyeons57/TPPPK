package com.example.domain.repository

import com.example.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 프로젝트 내 카테고리 관련 데이터 처리를 위한 인터페이스입니다.
 * 카테고리는 특정 프로젝트에 종속됩니다.
 */
interface CategoryRepository {
    /**
     * 특정 프로젝트에 속한 모든 카테고리 목록을 실시간 스트림으로 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 카테고리 목록을 담은 Result Flow. Firestore의 실시간 업데이트를 반영합니다.
     */
    fun getCategoriesStream(projectId: String): Flow<Result<List<Category>>>

    /**
     * 특정 ID를 가진 카테고리의 정보를 가져옵니다.
     * @param categoryId 카테고리 ID
     * @return 해당 카테고리 정보를 담은 Result.
     */
    suspend fun getCategory(categoryId: String): Result<Category>

    /**
     * 새로운 카테고리를 생성합니다.
     * @param categoryName 생성할 카테고리 이름
     * @param projectId 이 카테고리가 속할 프로젝트 ID
     * @return 생성된 카테고리의 ID를 담은 Result.
     */
    suspend fun createCategory(categoryName: String, projectId: String): Result<String>

    /**
     * 기존 카테고리 정보를 업데이트합니다. (예: 이름 변경)
     * @param categoryId 업데이트할 카테고리 ID
     * @param newName 새로운 카테고리 이름
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun updateCategoryName(categoryId: String, newName: String): Result<Unit>

    /**
     * 특정 카테고리를 삭제합니다.
     * @param categoryId 삭제할 카테고리 ID
     * @return 작업 성공 여부를 담은 Result.
     */
    suspend fun deleteCategory(categoryId: String): Result<Unit>

    /**
     * 특정 프로젝트 내 카테고리들의 순서를 업데이트합니다.
     * @param projectId 카테고리들이 속한 프로젝트 ID
     * @param categoryOrders 카테고리 ID와 새로운 순서(order) 값의 Map (예: mapOf(\
catId1\ to 0, \catId2\ to 1))
     * @return 작업 성공 여부를 담은 Result
     */
    suspend fun updateCategoryOrder(projectId: String, categoryOrders: Map<String, Int>): Result<Unit>
}
