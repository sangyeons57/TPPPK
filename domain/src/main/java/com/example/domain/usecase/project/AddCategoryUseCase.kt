package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.Category
import com.example.domain.model.collection.CategoryCollection
import java.util.UUID
import javax.inject.Inject

/**
 * 프로젝트 구조에 새 카테고리를 추가하는 유스케이스
 * 
 * 이 유스케이스는 프로젝트 구조에 새 카테고리를 추가하는 기능을 제공합니다.
 */
interface AddCategoryUseCase {
    /**
     * 프로젝트 구조에 새 카테고리를 추가합니다.
     * 
     * @param categories 현재 카테고리 컬렉션 목록
     * @param projectId 프로젝트 ID
     * @param categoryName 추가할 카테고리 이름 (기본값: "새 카테고리")
     * @return 카테고리가 추가된 카테고리 컬렉션 목록을 포함한 CustomResult
     */
    operator fun invoke(
        categories: List<CategoryCollection>,
        projectId: String,
        categoryName: String = "새 카테고리"
    ): CustomResult<List<CategoryCollection>, Exception>
}

/**
 * AddCategoryUseCase 구현체
 */
class AddCategoryUseCaseImpl @Inject constructor() : AddCategoryUseCase {
    
    /**
     * 프로젝트 구조에 새 카테고리를 추가합니다.
     * 
     * @param categories 현재 카테고리 컬렉션 목록
     * @param projectId 프로젝트 ID
     * @param categoryName 추가할 카테고리 이름 (기본값: "새 카테고리")
     * @return 카테고리가 추가된 카테고리 컬렉션 목록을 포함한 CustomResult
     */
    override operator fun invoke(
        categories: List<CategoryCollection>,
        projectId: String,
        categoryName: String
    ): CustomResult<List<CategoryCollection>, Exception> {
        try {
            val newCategoryId = UUID.randomUUID().toString()
            val now = DateTimeUtil.nowInstant()
            
            // 새 카테고리 생성
            val newCategory = Category(
                id = newCategoryId,
                name = categoryName,
                order = categories.size.toDouble(), // 새 카테고리는 마지막 순서
                createdBy = projectId, // 프로젝트 ID를 생성자로 설정
                createdAt = now,
                updatedAt = now
            )
            
            // 새 카테고리 컬렉션 생성 (빈 채널 목록과 함께)
            val newCategoryCollection = CategoryCollection(
                category = newCategory,
                channels = emptyList() // 새 카테고리는 채널이 없음
            )
            
            // 카테고리 컬렉션 목록에 새 카테고리 컬렉션 추가
            val updatedCategories = categories.toMutableList().apply {
                add(newCategoryCollection)
            }
            
            return CustomResult.Success(updatedCategories)
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }
}
