package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.collection.CategoryCollection
import javax.inject.Inject

/**
 * 카테고리 컬렉션 목록을 UI에서 사용할 수 있는 드래그 가능한 아이템 리스트로 변환하는 유스케이스
 * 
 * 이 유스케이스는 카테고리 컬렉션 목록(카테고리 및 채널)을 UI 레이어에서 드래그 앤 드롭 기능을 위한
 * 데이터 구조로 변환합니다. 변환된 결과는 UI 레이어에서 직접 사용할 수 있습니다.
 */
interface ConvertProjectStructureToDraggableItemsUseCase {
    /**
     * 카테고리 컬렉션 목록을 드래그 가능한 아이템 리스트로 변환합니다.
     *
     * @param categories 변환할 카테고리 컬렉션 목록
     * @return 드래그 가능한 아이템 리스트를 포함한 CustomResult
     */
    operator fun invoke(categories: List<CategoryCollection>): CustomResult<List<DraggableItem>, Exception>
}

/**
 * 드래그 가능한 아이템을 나타내는 데이터 클래스
 * UI 레이어에서 사용될 데이터 모델로, 카테고리와 채널 정보를 포함합니다.
 */
data class DraggableItem(
    val id: String,
    val depth: Int,
    val parentId: String?,
    val canAcceptChildren: Boolean,
    val maxRelativeChildDepth: Int,
    val item: DraggableItemType
)

/**
 * 드래그 가능한 아이템의 타입을 나타내는 sealed 인터페이스
 */
sealed interface DraggableItemType {
    val id: String
    val name: String

    /**
     * 카테고리 타입의 드래그 가능한 아이템
     */
    data class CategoryItem(val category: Category) : DraggableItemType {
        override val id: String get() = category.id
        override val name: String get() = category.name
    }

    /**
     * 채널 타입의 드래그 가능한 아이템
     */
    data class ChannelItem(
        val channel: ProjectChannel,
        val currentParentCategoryId: String
    ) : DraggableItemType {
        override val id: String get() = channel.id
        override val name: String get() = channel.channelName
    }
}

/**
 * ConvertProjectStructureToDraggableItemsUseCase의 구현체
 */
class ConvertProjectStructureToDraggableItemsUseCaseImpl @Inject constructor() : ConvertProjectStructureToDraggableItemsUseCase {
    
    /**
     * 카테고리 컬렉션 목록을 드래그 가능한 아이템 리스트로 변환합니다.
     *
     * @param categories 변환할 카테고리 컬렉션 목록
     * @return 드래그 가능한 아이템 리스트를 포함한 CustomResult
     */
    override operator fun invoke(categories: List<CategoryCollection>): CustomResult<List<DraggableItem>, Exception> {
        return try {
            val draggableItems = mutableListOf<DraggableItem>()
            
            // 카테고리와 해당 채널들을 추가
            categories.forEach { categoryCollection ->
                val category = categoryCollection.category
                
                // 카테고리 추가
                draggableItems.add(
                    DraggableItem(
                        id = category.id,
                        depth = 0,
                        parentId = null,
                        canAcceptChildren = true,
                        maxRelativeChildDepth = 1, // 카테고리는 채널을 자식으로 가질 수 있음
                        item = DraggableItemType.CategoryItem(category)
                    )
                )
                
                // 카테고리에 속한 채널들 추가
                categoryCollection.channels.sortedBy { it.order }.forEach { channel ->
                    draggableItems.add(
                        DraggableItem(
                            id = channel.id,
                            depth = 1,
                            parentId = category.id,
                            canAcceptChildren = false,
                            maxRelativeChildDepth = 0, // 채널은 자식을 가질 수 없음
                            item = DraggableItemType.ChannelItem(channel, category.id)
                        )
                    )
                }
            }
            
            CustomResult.Success(draggableItems)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
