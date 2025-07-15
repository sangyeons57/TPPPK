package com.example.feature_home.viewmodel.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.provider.project.ProjectStructureUseCaseProvider
import com.example.domain.provider.project.ProjectStructureUseCases
import javax.inject.Inject

/**
 * 카테고리 상태 관리를 담당하는 Service
 * Domain UseCase들을 조합하여 카테고리 관련 기능을 제공합니다.
 */
class CategoryManagementService @Inject constructor(
    private val projectStructureUseCaseProvider: ProjectStructureUseCaseProvider
) {
    
    private lateinit var projectStructureUseCases: ProjectStructureUseCases
    
    // 카테고리 확장 상태 캐시 (프로젝트 ID -> 카테고리 ID -> 확장 상태)
    private val categoryExpandedStates = mutableMapOf<String, MutableMap<String, Boolean>>()
    
    /**
     * 특정 프로젝트를 위한 UseCase 초기화
     */
    fun initializeForProject(projectId: DocumentId) {
        projectStructureUseCases = projectStructureUseCaseProvider.createForProject(projectId)
    }
    
    /**
     * 카테고리 확장 상태를 토글
     */
    fun toggleCategoryExpansion(projectId: DocumentId, categoryId: DocumentId): Boolean {
        val projectKey = projectId.value
        val categoryKey = categoryId.value
        
        val projectStates = categoryExpandedStates.getOrPut(projectKey) { mutableMapOf() }
        val currentState = projectStates[categoryKey] ?: false
        val newState = !currentState
        
        projectStates[categoryKey] = newState
        
        Log.d("CategoryManagementService", "Category $categoryKey expansion toggled to $newState")
        return newState
    }
    
    /**
     * 카테고리 확장 상태 조회
     */
    fun getCategoryExpansionState(projectId: DocumentId, categoryId: DocumentId): Boolean {
        val projectKey = projectId.value
        val categoryKey = categoryId.value
        
        return categoryExpandedStates[projectKey]?.get(categoryKey) ?: false
    }
    
    /**
     * 카테고리 확장 상태 설정
     */
    fun setCategoryExpansionState(projectId: DocumentId, categoryId: DocumentId, expanded: Boolean) {
        val projectKey = projectId.value
        val categoryKey = categoryId.value
        
        val projectStates = categoryExpandedStates.getOrPut(projectKey) { mutableMapOf() }
        projectStates[categoryKey] = expanded
        
        Log.d("CategoryManagementService", "Category $categoryKey expansion set to $expanded")
    }
    
    /**
     * 여러 카테고리의 확장 상태를 복원
     */
    fun restoreExpandedCategories(projectId: DocumentId, expandedCategoryIds: List<String>) {
        val projectKey = projectId.value
        val projectStates = categoryExpandedStates.getOrPut(projectKey) { mutableMapOf() }
        
        // 모든 카테고리를 일단 접힌 상태로 설정
        projectStates.clear()
        
        // 확장된 카테고리들을 설정
        expandedCategoryIds.forEach { categoryId ->
            projectStates[categoryId] = true
        }
        
        Log.d("CategoryManagementService", "Restored expanded categories: $expandedCategoryIds")
    }
    
    /**
     * 카테고리 순서 변경
     */
    suspend fun reorderCategories(
        projectId: DocumentId,
        reorderedCategories: List<Category>
    ): CustomResult<Unit, Exception> {
        Log.d("CategoryManagementService", "Reordering categories for project: $projectId")
        
        return try {
            if (!::projectStructureUseCases.isInitialized) {
                Log.w("CategoryManagementService", "ProjectStructureUseCases not initialized")
                CustomResult.Failure(IllegalStateException("Service not initialized"))
            } else {
                val categoryIds = reorderedCategories.map { it.id.value }
                projectStructureUseCases.reorderCategoriesUseCase(projectId, categoryIds)
            }
        } catch (e: Exception) {
            Log.e("CategoryManagementService", "Failed to reorder categories", e)
            CustomResult.Failure(e)
        }
    }
    
    /**
     * 채널 순서 변경
     */
    suspend fun reorderChannels(
        projectId: DocumentId,
        categoryId: DocumentId?,
        reorderedChannels: List<ProjectChannel>
    ): CustomResult<Unit, Exception> {
        Log.d("CategoryManagementService", "Reordering channels for project: $projectId, category: $categoryId")
        
        return try {
            if (!::projectStructureUseCases.isInitialized) {
                Log.w("CategoryManagementService", "ProjectStructureUseCases not initialized")
                CustomResult.Failure(IllegalStateException("Service not initialized"))
            } else {
                val channelIds = reorderedChannels.map { it.id.value }
                projectStructureUseCases.reorderChannelsUseCase(projectId, categoryId, channelIds)
            }
        } catch (e: Exception) {
            Log.e("CategoryManagementService", "Failed to reorder channels", e)
            CustomResult.Failure(e)
        }
    }
    
    /**
     * 프로젝트의 모든 카테고리 상태 클리어
     */
    fun clearCategoryStates(projectId: DocumentId) {
        val projectKey = projectId.value
        categoryExpandedStates.remove(projectKey)
        Log.d("CategoryManagementService", "Cleared category states for project: $projectKey")
    }
}