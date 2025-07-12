package com.example.domain.usecase.project.structure

import com.example.core_common.constants.Constants
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.base.ProjectChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
                when (categoryResult) {
                    is CustomResult.Success -> {
                        val categories = categoryResult.data
                            .filterIsInstance<Category>()
                            .filter { it.isCategory.value } // 실제 카테고리만 필터링
                            .sortedBy { it.order.value }
                        
                        // 각 카테고리별로 채널을 가져오고 통합 (Flow 반환)
                        buildProjectStructure(projectId, categories)
                    }
                    is CustomResult.Failure -> kotlinx.coroutines.flow.flowOf(CustomResult.Failure(categoryResult.error))
                    is CustomResult.Loading -> kotlinx.coroutines.flow.flowOf(CustomResult.Loading)
                    is CustomResult.Initial -> kotlinx.coroutines.flow.flowOf(CustomResult.Initial)
                    is CustomResult.Progress -> kotlinx.coroutines.flow.flowOf(CustomResult.Progress(categoryResult.progress))
                }
            }
    }

    /**
     * 카테고리 목록을 기반으로 프로젝트 구조를 빌드합니다.
     * 각 카테고리별로 채널을 가져오고 직접 채널도 포함합니다.
     * 실시간 업데이트를 위해 Flow를 반환합니다.
     */
    private fun buildProjectStructure(
        projectId: DocumentId,
        categories: List<Category>
    ): Flow<CustomResult<ProjectStructureData, Exception>> {
        
        // 모든 카테고리별 채널 Flow를 결합
        val categoryChannelFlows = categories.map { category ->
            getCategoryChannels(category.id)
                .map { channelResult ->
                    when (channelResult) {
                        is CustomResult.Success -> category to channelResult.data
                        is CustomResult.Failure -> category to emptyList<ProjectChannel>()
                        else -> category to emptyList<ProjectChannel>()
                    }
                }
        }

        // 직접 채널 Flow
        val directChannelFlow = getCategoryChannels(DocumentId(Constants.NO_CATEGORY_ID))
            .map { result ->
                when (result) {
                    is CustomResult.Success -> result.data
                    is CustomResult.Failure -> emptyList<ProjectChannel>()
                    else -> emptyList<ProjectChannel>()
                }
            }

        // 모든 Flow를 결합하여 ProjectStructureData 생성
        return if (categoryChannelFlows.isEmpty()) {
            // 카테고리가 없는 경우 직접 채널만 반환
            directChannelFlow.map { directChannels ->
                CustomResult.Success(
                    ProjectStructureData(
                        categoryChannelMap = emptyMap(),
                        directChannels = directChannels,
                        projectId = projectId
                    )
                )
            }
        } else {
            // 카테고리가 있는 경우 모든 데이터 결합
            combine(categoryChannelFlows + directChannelFlow) { results ->
                try {
                    val directChannels = results.last() as List<ProjectChannel>
                    val categoryChannelPairs = results.dropLast(1) as List<Pair<Category, List<ProjectChannel>>>
                    
                    val categoryChannelMap = categoryChannelPairs.toMap()
                    
                    CustomResult.Success(
                        ProjectStructureData(
                            categoryChannelMap = categoryChannelMap,
                            directChannels = directChannels,
                            projectId = projectId
                        )
                    )
                } catch (e: Exception) {
                    CustomResult.Failure(e)
                }
            }
        }
    }

    /**
     * 특정 카테고리의 채널들을 가져옵니다.
     */
    private fun getCategoryChannels(categoryId: DocumentId): Flow<CustomResult<List<ProjectChannel>, Exception>> {
        return projectChannelRepository.observeAll()
            .map { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val channels = result.data
                            .filterIsInstance<ProjectChannel>()
                            .filter { it.categoryId == categoryId }
                            .sortedBy { it.order.value }
                        CustomResult.Success(channels)
                    }
                    is CustomResult.Failure -> result
                    is CustomResult.Loading -> CustomResult.Loading
                    is CustomResult.Initial -> CustomResult.Initial
                    is CustomResult.Progress -> CustomResult.Progress(result.progress)
                }
            }
    }
}