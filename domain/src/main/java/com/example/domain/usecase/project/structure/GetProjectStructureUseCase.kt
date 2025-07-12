package com.example.domain.usecase.project.structure

import com.example.core_common.constants.Constants
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.base.ProjectChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Unified domain model that represents the complete project structure.
 * This aggregates categories and their associated channels into a single structure.
 * 
 * @property categoryChannelMap Map of categories to their channels
 * @property directChannels Channels that belong directly to the project (no category)
 * @property projectId The ID of the project this structure belongs to
 */
data class ProjectStructureData(
    val categoryChannelMap: Map<Category, List<ProjectChannel>> = emptyMap(),
    val directChannels: List<ProjectChannel> = emptyList(),
    val projectId: DocumentId
) {
    
    /**
     * Gets all categories in the structure.
     */
    fun getAllCategories(): List<Category> = categoryChannelMap.keys.toList()
    
    /**
     * Gets all channels for a specific category.
     */
    fun getChannelsForCategory(category: Category): List<ProjectChannel> = 
        categoryChannelMap[category] ?: emptyList()
    
    /**
     * Gets all channels in the project (both categorized and direct).
     */
    fun getAllChannels(): List<ProjectChannel> = 
        categoryChannelMap.values.flatten() + directChannels
    
    /**
     * Checks if the structure is empty (no categories or direct channels).
     */
    fun isEmpty(): Boolean = categoryChannelMap.isEmpty() && directChannels.isEmpty()
    
    /**
     * Gets the total count of channels in the project.
     */
    fun getTotalChannelCount(): Int = 
        categoryChannelMap.values.sumOf { it.size } + directChannels.size
    
    /**
     * Gets the total count of categories in the project.
     */
    fun getCategoryCount(): Int = categoryChannelMap.size
    
    companion object {
        /**
         * Creates an empty ProjectStructureData instance.
         */
        fun empty(projectId: DocumentId): ProjectStructureData = 
            ProjectStructureData(projectId = projectId)
    }
}

/**
 * 프로젝트의 전체 구조(카테고리 + 채널)를 통합하여 가져오는 유스케이스 인터페이스
 * 기존의 분리된 카테고리/채널 로딩 로직을 통합하여 단일 데이터 구조로 반환합니다.
 */
interface GetProjectStructureUseCase {
    /**
     * 프로젝트의 전체 구조를 통합된 형태로 반환합니다.
     * @param projectId 프로젝트 ID
     * @return Flow<CustomResult<ProjectStructureData, Exception>> 프로젝트 구조 데이터
     */
    suspend operator fun invoke(projectId: DocumentId): Flow<CustomResult<ProjectStructureData, Exception>>
}

/**
 * GetProjectStructureUseCase의 구현체
 * CategoryRepository와 ProjectChannelRepository를 사용하여 통합된 구조를 생성합니다.
 */
class GetProjectStructureUseCaseImpl @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val projectChannelRepository: ProjectChannelRepository
) : GetProjectStructureUseCase {

    /**
     * 프로젝트의 전체 구조를 통합하여 가져옵니다.
     * 
     * 1. 모든 카테고리를 가져옵니다.
     * 2. 각 카테고리별로 채널을 가져옵니다.
     * 3. 직접 채널(NoCategory)을 가져옵니다.
     * 4. 모든 데이터를 ProjectStructureData로 통합합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return Flow<CustomResult<ProjectStructureData, Exception>> 통합된 프로젝트 구조
     */
    override suspend fun invoke(projectId: DocumentId): Flow<CustomResult<ProjectStructureData, Exception>> {
        return categoryRepository.observeAll()
            .flatMapLatest { categoryResult ->
                android.util.Log.d("GetProjectStructureUseCase", "Category result for project ${projectId.value}: $categoryResult")
                when (categoryResult) {
                    is CustomResult.Success -> {
                        val categories = categoryResult.data
                            .filterIsInstance<Category>()
                            .filter { it.isCategory.value } // 실제 카테고리만 필터링
                            .sortedBy { it.order.value }
                        
                        android.util.Log.d("GetProjectStructureUseCase", "Found ${categories.size} categories for project ${projectId.value}")
                        
                        // 각 카테고리별로 채널을 가져오고 통합 (Flow 반환)
                        buildProjectStructure(projectId, categories)
                    }
                    is CustomResult.Failure -> {
                        android.util.Log.e("GetProjectStructureUseCase", "Failed to load categories for project ${projectId.value}", categoryResult.error)
                        kotlinx.coroutines.flow.flowOf(CustomResult.Failure(categoryResult.error))
                    }
                    is CustomResult.Loading -> kotlinx.coroutines.flow.flowOf(CustomResult.Loading)
                    is CustomResult.Initial -> kotlinx.coroutines.flow.flowOf(CustomResult.Initial)
                    is CustomResult.Progress -> kotlinx.coroutines.flow.flowOf(CustomResult.Progress(categoryResult.progress))
                }
            }
    }

    /**
     * 카테고리 목록을 기반으로 프로젝트 구조를 빌드합니다.
     * 모든 채널을 한 번에 가져와서 categoryId로 그룹핑합니다.
     * 실시간 업데이트를 위해 Flow를 반환합니다.
     */
    private fun buildProjectStructure(
        projectId: DocumentId,
        categories: List<Category>
    ): Flow<CustomResult<ProjectStructureData, Exception>> {
        
        // 모든 프로젝트 채널을 한 번에 가져와서 groupBy로 분류
        return projectChannelRepository.observeAll()
            .map { channelResult ->
                android.util.Log.d("GetProjectStructureUseCase", "Channel result for project ${projectId.value}: $channelResult")
                when (channelResult) {
                    is CustomResult.Success -> {
                        try {
                            val allChannels = channelResult.data.filterIsInstance<ProjectChannel>()
                            android.util.Log.d("GetProjectStructureUseCase", "Found ${allChannels.size} channels for project ${projectId.value}")
                            
                            // categoryId로 채널들을 그룹핑
                            val channelsByCategory = allChannels.groupBy { it.categoryId.value }
                            android.util.Log.d("GetProjectStructureUseCase", "Channels grouped by category: ${channelsByCategory.keys}")
                            
                            // 직접 채널 (NoCategory)
                            val directChannels = channelsByCategory[Category.NO_CATEGORY_ID] ?: emptyList()
                            android.util.Log.d("GetProjectStructureUseCase", "Direct channels count: ${directChannels.size}")
                            
                            // 카테고리별 채널 맵 생성
                            val categoryChannelMap = mutableMapOf<Category, List<ProjectChannel>>()
                            categories.forEach { category ->
                                val channels = channelsByCategory[category.id.value] ?: emptyList()
                                if (channels.isNotEmpty()) {
                                    categoryChannelMap[category] = channels.sortedBy { it.order.value }
                                    android.util.Log.d("GetProjectStructureUseCase", "Category ${category.name.value} has ${channels.size} channels")
                                }
                            }
                            
                            val result = ProjectStructureData(
                                categoryChannelMap = categoryChannelMap.toMap(),
                                directChannels = directChannels.sortedBy { it.order.value },
                                projectId = projectId
                            )
                            
                            android.util.Log.d("GetProjectStructureUseCase", "Built project structure: ${categoryChannelMap.size} categories with channels, ${directChannels.size} direct channels")
                            
                            CustomResult.Success(result)
                        } catch (e: Exception) {
                            android.util.Log.e("GetProjectStructureUseCase", "Error building project structure for ${projectId.value}", e)
                            CustomResult.Failure(e)
                        }
                    }
                    is CustomResult.Failure -> {
                        android.util.Log.e("GetProjectStructureUseCase", "Failed to load channels for project ${projectId.value}", channelResult.error)
                        channelResult
                    }
                    is CustomResult.Loading -> CustomResult.Loading
                    is CustomResult.Initial -> CustomResult.Initial
                    is CustomResult.Progress -> CustomResult.Progress(channelResult.progress)
                }
            }
    }

}