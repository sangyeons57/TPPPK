package com.example.core_ui.dialogs.adapter

import com.example.core_common.result.CustomResult
import com.example.core_ui.components.draggablelist.DraggableListItemData
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.usecase.project.structure.ConvertProjectStructureToDraggableItemsUseCase
import com.example.domain.usecase.project.structure.DraggableItem
import com.example.domain.usecase.project.structure.DraggableItemType

import javax.inject.Inject

/**
 * 도메인 레이어의 ConvertProjectStructureToDraggableItemsUseCase를 UI 레이어에서 사용할 수 있도록
 * 변환해주는 어댑터 클래스입니다.
 * 
 * 이 어댑터는 도메인 레이어의 DraggableItem을 UI 레이어의 DraggableListItemData로 변환합니다.
 */
class ProjectStructureToDraggableItemsAdapter @Inject constructor(
    private val convertProjectStructureToDraggableItemsUseCase: ConvertProjectStructureToDraggableItemsUseCase
) {
    /**
     * 카테고리 컬렉션 목록을 UI에서 사용할 수 있는 드래그 가능한 아이템 리스트로 변환합니다.
     *
     * @param categories 변환할 카테고리 컬렉션 목록
     * @return 드래그 가능한 아이템 리스트를 포함한 Result
     */
    operator fun invoke(categories: List<CategoryCollection>): CustomResult<List<DraggableListItemData<ProjectStructureDraggableItem>>, Exception> {
        val result = convertProjectStructureToDraggableItemsUseCase(categories)
        
        return when (result) {
            is CustomResult.Success -> {
                val uiItems = result.data.map { domainItem ->
                    mapDomainItemToUiItem(domainItem)
                }
                CustomResult.Success(uiItems)
            }
            is CustomResult.Failure -> {
                CustomResult.Failure(result.error)
            }
            else -> {
                CustomResult.Failure(Exception("Unknown result type"))
            }
        }
    }
    
    /**
     * 도메인 레이어의 DraggableItem을 UI 레이어의 DraggableListItemData로 변환합니다.
     *
     * @param domainItem 도메인 레이어의 드래그 가능한 아이템
     * @return UI 레이어의 드래그 가능한 아이템
     */
    private fun mapDomainItemToUiItem(
        domainItem: DraggableItem
    ): DraggableListItemData<ProjectStructureDraggableItem> {
        val uiData = when (val itemType = domainItem.item) {
            is DraggableItemType.CategoryItem -> {
                ProjectStructureDraggableItem.CategoryDraggable(itemType.category)
            }
            is DraggableItemType.ChannelItem -> {
                ProjectStructureDraggableItem.ChannelDraggable(
                    itemType.channel,
                    itemType.currentParentCategoryId
                )
            }
        }
        
        return DraggableListItemData(
            id = domainItem.id,
            originalData = uiData,
            depth = domainItem.depth,
            parentId = domainItem.parentId,
            canAcceptChildren = domainItem.canAcceptChildren,
            maxRelativeChildDepth = domainItem.maxRelativeChildDepth
        )
    }
}

/**
 * 프로젝트 구조에서 드래그 가능한 아이템을 나타내는 sealed class입니다.
 * 카테고리와 채널을 표현할 수 있습니다.
 */
sealed class ProjectStructureDraggableItem {
    /**
     * 드래그 가능한 카테고리 아이템
     * 
     * @param category 카테고리 정보
     */
    data class CategoryDraggable(val category: Category) : ProjectStructureDraggableItem()
    
    /**
     * 드래그 가능한 채널 아이템
     * 
     * @param channel 채널 정보
     * @param currentParentCategoryId 현재 부모 카테고리 ID
     */
    data class ChannelDraggable(
        val channel: ProjectChannel,
        val currentParentCategoryId: String
    ) : ProjectStructureDraggableItem()
}