package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.collection.CategoryCollection
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트의 카테고리 컬렉션(카테고리와 해당 카테고리에 속한 채널 목록)을 관리하는 리포지토리 인터페이스
 * 이 리포지토리는 CategoryRepository와 ProjectChannelRepository의 데이터를 조합하여
 * 카테고리와 채널을 함께 관리하는 고수준 인터페이스를 제공합니다.
 */
interface CategoryCollectionRepository {
    /**
     * 특정 프로젝트의 모든 카테고리와 각 카테고리에 속한 채널 목록을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return Flow<CustomResult<List<CategoryCollection>, Exception>> 카테고리 컬렉션 목록을 포함한 결과
     */
    suspend fun getCategoryCollections(projectId: String): Flow<CustomResult<List<CategoryCollection>, Exception>>
    
    /**
     * 카테고리 컬렉션 목록을 업데이트합니다.
     * 이 메서드는 카테고리와 채널의 구조적 변경(순서 변경, 부모-자식 관계 변경 등)을 처리합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param collections 업데이트할 카테고리 컬렉션 목록
     * @return CustomResult<Unit, Exception> 업데이트 결과
     */
    suspend fun updateCategoryCollections(projectId: String, collections: List<CategoryCollection>): CustomResult<Unit, Exception>
    
    /**
     * 특정 카테고리에 새 채널을 추가합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channel 추가할 채널 정보
     * @return CustomResult<CategoryCollection, Exception> 업데이트된 카테고리 컬렉션
     */
    suspend fun addChannelToCategory(projectId: String, categoryId: String, channel: ProjectChannel): CustomResult<CategoryCollection, Exception>
    
    /**
     * 특정 카테고리에서 채널을 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 삭제할 채널 ID
     * @return CustomResult<CategoryCollection, Exception> 업데이트된 카테고리 컬렉션
     */
    suspend fun removeChannelFromCategory(projectId: String, categoryId: String, channelId: String): CustomResult<CategoryCollection, Exception>
    
    /**
     * 채널을 한 카테고리에서 다른 카테고리로 이동합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelId 이동할 채널 ID
     * @param sourceCategoryId 원본 카테고리 ID
     * @param targetCategoryId 대상 카테고리 ID
     * @param newOrder 새 순서 (대상 카테고리 내에서의 위치)
     * @return CustomResult<List<CategoryCollection>, Exception> 업데이트된 카테고리 컬렉션 목록
     */
    suspend fun moveChannelBetweenCategories(
        projectId: String,
        channelId: String,
        sourceCategoryId: String,
        targetCategoryId: String,
        newOrder: Int
    ): CustomResult<List<CategoryCollection>, Exception>
    
    /**
     * 카테고리 순서를 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 이동할 카테고리 ID
     * @param newOrder 새 순서
     * @return CustomResult<List<CategoryCollection>, Exception> 업데이트된 카테고리 컬렉션 목록
     */
    suspend fun moveCategoryOrder(
        projectId: String,
        categoryId: String,
        newOrder: Int
    ): CustomResult<List<CategoryCollection>, Exception>
    
    /**
     * 새 카테고리를 추가합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param category 추가할 카테고리 정보
     * @return CustomResult<CategoryCollection, Exception> 추가된 카테고리 컬렉션
     */
    suspend fun addCategory(projectId: String, category: Category): CustomResult<CategoryCollection, Exception>
    
    /**
     * 카테고리를 삭제합니다. 카테고리에 속한 모든 채널도 함께 삭제됩니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 삭제할 카테고리 ID
     * @return CustomResult<Unit, Exception> 삭제 결과
     */
    suspend fun removeCategory(projectId: String, categoryId: String): CustomResult<Unit, Exception>
    
    /**
     * 카테고리 이름을 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param newName 새 이름
     * @return CustomResult<CategoryCollection, Exception> 업데이트된 카테고리 컬렉션
     */
    suspend fun renameCategory(projectId: String, categoryId: String, newName: String): CustomResult<CategoryCollection, Exception>
    
    /**
     * 채널 이름을 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @param newName 새 이름
     * @return CustomResult<CategoryCollection, Exception> 업데이트된 카테고리 컬렉션
     */
    suspend fun renameChannel(projectId: String, categoryId: String, channelId: String, newName: String): CustomResult<CategoryCollection, Exception>
}
