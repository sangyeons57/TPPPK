package com.example.domain.usecase.project.structure

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.base.ProjectChannelRepository
import javax.inject.Inject

/**
 * 카테고리와 직속 채널의 통합된 순서 관리를 위한 UseCase
 * 프로젝트 구조에서 카테고리와 No_Category 채널들을 하나의 통합된 순서로 관리합니다.
 */
interface ReorderUnifiedProjectStructureUseCase {
    /**
     * 카테고리와 직속 채널의 통합 순서를 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param unifiedItems 통합된 순서의 아이템 목록 (카테고리 ID 또는 채널 ID)
     * @param itemTypes 각 아이템의 타입 ("category" 또는 "channel")
     * @return 순서 변경 결과
     */
    suspend operator fun invoke(
        projectId: DocumentId,
        unifiedItems: List<String>,
        itemTypes: List<String>
    ): CustomResult<Unit, Exception>
}

/**
 * ReorderUnifiedProjectStructureUseCase의 구현체
 * 
 * @property categoryRepository 카테고리 Repository
 * @property projectChannelRepository 채널 Repository  
 */
class ReorderUnifiedProjectStructureUseCaseImpl @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val projectChannelRepository: ProjectChannelRepository
) : ReorderUnifiedProjectStructureUseCase {

    override suspend fun invoke(
        projectId: DocumentId,
        unifiedItems: List<String>,
        itemTypes: List<String>
    ): CustomResult<Unit, Exception> {
        try {
            // 입력 검증
            if (unifiedItems.size != itemTypes.size) {
                return CustomResult.Failure(
                    IllegalArgumentException("unifiedItems and itemTypes must have the same size")
                )
            }

            // 모든 카테고리와 직속 채널 조회
            val categoriesResult = categoryRepository.findAll()
            if (categoriesResult !is CustomResult.Success) {
                return CustomResult.Failure(Exception("Failed to fetch categories"))
            }

            val channelsResult = projectChannelRepository.findAll()
            if (channelsResult !is CustomResult.Success) {
                return CustomResult.Failure(Exception("Failed to fetch channels"))
            }

            val allCategories = categoriesResult.data.filterIsInstance<Category>()
            val allChannels = channelsResult.data.filterIsInstance<ProjectChannel>()
            
            // No_Category 채널들만 필터링 (직속 채널)
            val directChannels = allChannels.filter { it.categoryId.value == Category.NO_CATEGORY_ID }

            // 카테고리와 직속 채널의 ID 맵 생성
            val categoryMap = allCategories.associateBy { it.id.value }
            val directChannelMap = directChannels.associateBy { it.id.value }

            // 통합 순서에 따라 globalOrder 계산 및 업데이트
            val categoryUpdates = mutableListOf<Pair<Category, CategoryOrder>>()
            val channelUpdates = mutableListOf<Pair<ProjectChannel, ProjectChannelOrder>>()

            unifiedItems.forEachIndexed { index, itemId ->
                val itemType = itemTypes[index]
                val globalOrder = index.toDouble()

                when (itemType) {
                    "category" -> {
                        val category = categoryMap[itemId]
                            ?: return CustomResult.Failure(
                                IllegalArgumentException("Category not found: $itemId")
                            )
                        
                        // No_Category는 항상 order 0.0으로 고정
                        val newOrder = if (itemId == Category.NO_CATEGORY_ID) {
                            CategoryOrder(Category.NO_CATEGORY_ORDER)
                        } else {
                            CategoryOrder(globalOrder.toInt())
                        }
                        
                        categoryUpdates.add(category to newOrder)
                    }
                    
                    "channel" -> {
                        val channel = directChannelMap[itemId]
                            ?: return CustomResult.Failure(
                                IllegalArgumentException("Direct channel not found: $itemId")
                            )
                        
                        // 직속 채널은 globalOrder를 그대로 사용 (No_Category 채널)
                        val newOrder = ProjectChannelOrder(globalOrder.toInt())
                        channelUpdates.add(channel to newOrder)
                    }
                    
                    else -> {
                        return CustomResult.Failure(
                            IllegalArgumentException("Invalid item type: $itemType. Must be 'category' or 'channel'")
                        )
                    }
                }
            }

            // 카테고리 순서 업데이트
            categoryUpdates.forEach { (category, newOrder) ->
                category.changeOrder(newOrder)
                when (val saveResult = categoryRepository.save(category)) {
                    is CustomResult.Failure -> return CustomResult.Failure(saveResult.error)
                    else -> { /* 성공 */ }
                }
            }

            // 직속 채널 순서 업데이트
            channelUpdates.forEach { (channel, newOrder) ->
                channel.changeOrder(newOrder)
                when (val saveResult = projectChannelRepository.save(channel)) {
                    is CustomResult.Failure -> return CustomResult.Failure(saveResult.error)
                    else -> { /* 성공 */ }
                }
            }

            return CustomResult.Success(Unit)

        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }
}