
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.model.remote.CategoryDTO
import kotlinx.coroutines.flow.Flow

interface CategoryRemoteDataSource {

    /**
     * 특정 프로젝트의 모든 카테고리 목록을 순서대로 실시간 관찰합니다.
     * @param projectId 카테고리를 가져올 프로젝트의 ID
     */
    fun observeCategories(projectId: String): Flow<CustomResult<List<CategoryDTO>, Exception>>

    fun observeCategory(projectId: String, categoryId: String): Flow<CustomResult<CategoryDTO, Exception>>

    /**
     * 프로젝트에 새로운 카테고리를 추가합니다.
     * @param projectId 카테고리를 추가할 프로젝트의 ID
     * @param name 새로운 카테고리의 이름
     * @param order 카테고리의 순서
     * @return 생성된 카테고리의 ID를 포함한 Result 객체
     */
    suspend fun addCategory(projectId: String, categoryDTO: CategoryDTO): CustomResult<String, Exception>

    suspend fun setDirectCategory(projectId: String, categoryId: String, categoryDTO: CategoryDTO): CustomResult<Unit, Exception>

    /**
     * 카테고리의 이름 또는 순서를 수정합니다.
     * @param projectId 대상 프로젝트의 ID
     * @param categoryId 수정할 카테고리의 ID
     * @param newName 새로운 이름
     * @param newOrder 새로운 순서
     */
    suspend fun updateCategory(
        projectId: String,
        categoryId: String,
        newName: String,
        newOrder: Double
    ): CustomResult<Unit, Exception>

    /**
     * 프로젝트에서 카테고리를 삭제합니다.
     * @param projectId 대상 프로젝트의 ID
     * @param categoryId 삭제할 카테고리의 ID
     */
    suspend fun deleteCategory(projectId: String, categoryId: String): CustomResult<Unit, Exception>
}

