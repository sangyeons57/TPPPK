package com.example.mapper.category

import com.example.data_model.remote.CategoryDTO
import com.example.domain.model.base.Category
import com.example.domain.vo.DocumentId
import com.example.domain.vo.OwnerId
import com.example.domain.vo.category.CategoryName
import com.example.domain.vo.category.CategoryOrder
import com.example.domain.vo.category.IsCategoryFlag
import com.example.mapper.DtoMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Category 관련 Domain과 DTO 간의 매핑을 담당하는 Mapper
 */
@Singleton
class CategoryMapper @Inject constructor() : DtoMapper<Category, CategoryDTO> {

    override fun dtoToDomain(dto: CategoryDTO): Category {
        return Category.fromDataSource(
            id = DocumentId(dto.id),
            name = CategoryName(dto.name),
            order = CategoryOrder.fromDouble(dto.order),
            createdBy = OwnerId(dto.createdBy),
            isCategory = IsCategoryFlag(dto.isCategory),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant()
        )
    }

    override fun domainToDto(domain: Category): CategoryDTO {
        return CategoryDTO(
            id = domain.id.value,
            name = domain.name.value,
            order = domain.order.toDouble(),
            createdBy = domain.createdBy.value,
            isCategory = domain.isCategory.value,
            createdAt = null, // ServerTimestamp가 처리
            updatedAt = null  // ServerTimestamp가 처리
        )
    }
}