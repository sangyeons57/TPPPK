package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.collection.CategoryCollection
import javax.inject.Inject

/**
 * 카테고리 컬렉션 목록 내에서 카테고리를 이동하는 유스케이스
 * 
 * 이 유스케이스는 카테고리의 순서를 변경하는 기능을 제공합니다.
 */
interface MoveCategoryUseCase {
    /**
     * 카테고리를 이동합니다.
     * 
     * @param categories 현재 카테고리 컬렉션 목록
     * @param fromIndex 이동할 카테고리의 현재 인덱스
     * @param toIndex 이동할 목표 인덱스
     * @return 이동 결과를 포함한 CustomResult
     */
    suspend operator fun invoke(
        categories: List<CategoryCollection>,
        fromIndex: Int,
        toIndex: Int
    ): CustomResult<List<CategoryCollection>, Exception>
}

/**
 * MoveCategoryUseCase 구현체
 */
class MoveCategoryUseCaseImpl @Inject constructor() : MoveCategoryUseCase {
    
    /**
     * 카테고리를 이동합니다.
     * 
     * @param categories 현재 카테고리 컬렉션 목록
     * @param fromIndex 이동할 카테고리의 현재 인덱스
     * @param toIndex 이동할 목표 인덱스
     * @return 이동 결과를 포함한 CustomResult
     */
    override suspend operator fun invoke(
        categories: List<CategoryCollection>,
        fromIndex: Int,
        toIndex: Int
    ): CustomResult<List<CategoryCollection>, Exception> {
        try {
            // 변경 사항이 없는 경우 검증
            if (fromIndex == toIndex) {
                return CustomResult.Failure(Exception("fromIndex and toIndex are the same"))
            }
            
            // 인덱스 범위 검증
            if (fromIndex < 0 || fromIndex >= categories.size || 
                toIndex < 0 || toIndex > categories.size) {
                return CustomResult.Failure(Exception("Invalid index"))
            }

            // 현재 카테고리 컬렉션 목록 복사
            val currentCategories = categories.toMutableList()
            
            // 카테고리 컬렉션 이동
            val movedCategoryCollection = currentCategories.removeAt(fromIndex)
            currentCategories.add(toIndex, movedCategoryCollection)
            
            // 순서 변경에 따른 order 필드 업데이트
            val now = DateTimeUtil.nowInstant()
            val updatedCategories = currentCategories.mapIndexed { index, categoryCollection ->
                // 카테고리의 order와 updatedAt 필드만 업데이트
                val updatedCategory = categoryCollection.category.copy(
                    order = index.toDouble(), 
                    updatedAt = now
                )
                
                // 채널은 그대로 유지하고 카테고리만 업데이트된 새 CategoryCollection 반환
                CategoryCollection(
                    category = updatedCategory,
                    channels = categoryCollection.channels
                )
            }
            
            return CustomResult.Success(updatedCategories)
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }
}
