package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.collection.CategoryCollection
import javax.inject.Inject

/**
 * 카테고리 컬렉션 목록 내에서 채널을 이동하는 유스케이스
 * 
 * 이 유스케이스는 채널을 한 카테고리에서 다른 카테고리로 이동하는 기능을 제공합니다.
 */
interface MoveChannelUseCase {
    /**
     * 채널을 이동합니다.
     * 
     * @param categories 현재 카테고리 컬렉션 목록
     * @param fromCategoryId 원본 카테고리 ID
     * @param fromChannelId 이동할 채널 ID
     * @param fromIndex 이동할 채널의 현재 인덱스
     * @param toCategoryId 대상 카테고리 ID
     * @param toIndex 이동할 목표 인덱스
     * @return 이동 결과를 포함한 CustomResult
     */
    suspend operator fun invoke(
        categories: List<CategoryCollection>,
        fromCategoryId: String,
        fromChannelId: String,
        fromIndex: Int,
        toCategoryId: String,
        toIndex: Int
    ): CustomResult<List<CategoryCollection>, Exception>
}

/**
 * MoveChannelUseCase 구현체
 */
class MoveChannelUseCaseImpl @Inject constructor() : MoveChannelUseCase {
    
    /**
     * 채널을 이동합니다.
     * 
     * @param categories 현재 카테고리 컬렉션 목록
     * @param fromCategoryId 원본 카테고리 ID
     * @param fromChannelId 이동할 채널 ID
     * @param fromIndex 이동할 채널의 현재 인덱스
     * @param toCategoryId 대상 카테고리 ID
     * @param toIndex 이동할 목표 인덱스
     * @return 이동 결과를 포함한 CustomResult
     */
    override suspend operator fun invoke(
        categories: List<CategoryCollection>,
        fromCategoryId: String,
        fromChannelId: String,
        fromIndex: Int,
        toCategoryId: String,
        toIndex: Int
    ): CustomResult<List<CategoryCollection>, Exception> {
        try {
            // 변경 사항이 없는 경우 검증
            if (fromCategoryId == toCategoryId && fromIndex == toIndex) {
                return CustomResult.Failure(Exception("No change within the same category"))
            }

            val now = DateTimeUtil.nowInstant()
            var movedChannel: ProjectChannel? = null
            
            // 카테고리 컬렉션 복사
            val currentCategories = categories.toMutableList()

            // 1. 원본 카테고리 검색
            val sourceCategoryIndex = currentCategories.indexOfFirst { it.category.id.value == fromCategoryId }
            if (sourceCategoryIndex == -1) {
                return CustomResult.Failure(Exception("Source category not found"))
            }
            
            // 2. 대상 카테고리 검색
            val targetCategoryIndex = currentCategories.indexOfFirst { it.category.id.value == toCategoryId }
            if (targetCategoryIndex == -1) {
                return CustomResult.Failure(Exception("Target category not found"))
            }
            
            // 3. 원본 카테고리에서 채널 찾기 및 제거
            val sourceCategory = currentCategories[sourceCategoryIndex]
            val mutableSourceChannels = sourceCategory.channels.toMutableList()
            
            movedChannel = mutableSourceChannels.find { it.id == fromChannelId }
                ?: return CustomResult.Failure(Exception("Channel not found in source category"))
            
            mutableSourceChannels.remove(movedChannel)
            
            // 4. 원본 카테고리의 채널 순서 재정렬
            val updatedSourceChannels = mutableSourceChannels.mapIndexed { idx, ch -> 
                ch.copy(order = idx.toDouble(), updatedAt = now)
            }
            
            // 5. 원본 카테고리 업데이트
            val updatedSourceCategory = sourceCategory.category.copy(updatedAt = now)
            currentCategories[sourceCategoryIndex] = CategoryCollection(
                category = updatedSourceCategory,
                channels = updatedSourceChannels
            )
            
            // 6. 대상 카테고리에 채널 추가
            val targetCategory = currentCategories[targetCategoryIndex]
            val mutableTargetChannels = targetCategory.channels.toMutableList()
            
            // 7. 이동할 채널 업데이트
            val updatedMovedChannel = movedChannel.copy(
                channelName = movedChannel.channelName,
                order = toIndex.toDouble(),
                updatedAt = now
            )
            
            // 8. 대상 카테고리에 채널 추가
            mutableTargetChannels.add(
                toIndex.coerceIn(0, mutableTargetChannels.size), 
                updatedMovedChannel
            )
            
            // 9. 대상 카테고리의 채널 순서 재정렬
            val updatedTargetChannels = mutableTargetChannels.mapIndexed { idx, ch -> 
                ch.copy(order = idx.toDouble(), updatedAt = now)
            }
            
            // 10. 대상 카테고리 업데이트
            val updatedTargetCategory = targetCategory.category.copy(updatedAt = now)
            currentCategories[targetCategoryIndex] = CategoryCollection(
                category = updatedTargetCategory,
                channels = updatedTargetChannels
            )
            
            return CustomResult.Success(currentCategories)
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }
}
