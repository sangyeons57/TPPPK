package com.example.data.mapper

import com.example.data.model.local.CategoriesEntity
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.model.vo.category.IsCategoryFlag

/**
 * CategoriesEntity와 Category 도메인 모델 간의 변환을 담당하는 매퍼
 * 3-tier 동기화 아키텍처에서 Entity와 Domain 모델 간 변환을 처리합니다
 */
object CategoriesMapper {

    /**
     * Category 도메인 모델을 CategoriesEntity로 변환
     * @param category 변환할 Category 도메인 모델
     * @param projectId 카테고리가 속한 프로젝트 ID
     * @return CategoriesEntity
     */
    fun toEntity(category: Category, projectId: String): CategoriesEntity {
        return CategoriesEntity(
            id = category.id.value,
            projectId = projectId,
            name = category.name.value,
            order = category.order.value,
            createdBy = category.createdBy.value,
            isCategory = category.isCategory.value,
            createdAt = category.createdAt,
            updatedAt = category.updatedAt
        )
    }

    /**
     * CategoriesEntity를 Category 도메인 모델로 변환
     * @param entity 변환할 CategoriesEntity
     * @return Category 도메인 모델
     */
    fun toDomain(entity: CategoriesEntity): Category {
        return Category.fromDataSource(
            id = DocumentId(entity.id),
            name = CategoryName(entity.name),
            order = CategoryOrder(entity.order),
            createdBy = OwnerId(entity.createdBy),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isCategory = IsCategoryFlag.fromBoolean(entity.isCategory)
        )
    }

    /**
     * Category 도메인 모델 리스트를 CategoriesEntity 리스트로 변환
     * @param categories 변환할 Category 도메인 모델 리스트
     * @param projectId 카테고리들이 속한 프로젝트 ID
     * @return CategoriesEntity 리스트
     */
    fun toEntityList(categories: List<Category>, projectId: String): List<CategoriesEntity> {
        return categories.map { toEntity(it, projectId) }
    }

    /**
     * CategoriesEntity 리스트를 Category 도메인 모델 리스트로 변환
     * @param entities 변환할 CategoriesEntity 리스트
     * @return Category 도메인 모델 리스트
     */
    fun toDomainList(entities: List<CategoriesEntity>): List<Category> {
        return entities.map { toDomain(it) }
    }
}