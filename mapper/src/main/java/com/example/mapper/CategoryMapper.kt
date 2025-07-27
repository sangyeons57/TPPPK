package com.example.mapper

import com.example.data_model.remote.CategoryDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.model.vo.category.IsCategoryFlag
import com.example.mapper.base.BaseMapper
import java.util.Date

interface CategoryMapper : BaseMapper<Category, CategoryDTO>

class CategoryMapperImpl : CategoryMapper {
    override fun toDomain(dto: CategoryDTO): Category {
        return Category.fromDataSource(
            id = DocumentId(dto.id),
            name = CategoryName(dto.name),
            order = CategoryOrder.fromDouble(dto.order),
            createdBy = OwnerId(dto.createdBy),
            createdAt = dto.createdAt?.toInstant(),
            updatedAt = dto.updatedAt?.toInstant(),
            isCategory = IsCategoryFlag(dto.isCategory)
        )
    }

    override fun toDto(domain: Category): CategoryDTO {
        return CategoryDTO(
            id = domain.id.value,
            name = domain.name.value,
            order = domain.order.value.toDouble(),
            createdBy = domain.createdBy.value,
            createdAt = null,
            updatedAt = null,
            isCategory = domain.isCategory.value
        )
    }

    override fun domainToMap(domain: Category): Map<String, Any?> {
        return mapOf(
            Category.KEY_NAME to domain.name.value,
            Category.KEY_ORDER to domain.order.value,
            Category.KEY_CREATED_BY to domain.createdBy.value,
            Category.KEY_IS_CATEGORY to domain.isCategory.value
        )
    }

    override fun dataToMap(data: CategoryDTO): Map<String, Any?> {
        return mapOf(
            Category.KEY_NAME to data.name,
            Category.KEY_ORDER to data.order,
            Category.KEY_CREATED_BY to data.createdBy,
            Category.KEY_IS_CATEGORY to data.isCategory
        )
    }

    override fun mapToDto(map: Map<String, Any?>): CategoryDTO {
        return CategoryDTO(
            id = map["id"] as? String ?: "",
            name = map[Category.KEY_NAME] as? String ?: "",
            order = map[Category.KEY_ORDER] as? Double ?: 0.0,
            createdBy = map[Category.KEY_CREATED_BY] as? String ?: "",
            isCategory = map[Category.KEY_IS_CATEGORY] as? Boolean ?: true,
            createdAt = map[AggregateRoot.KEY_CREATED_AT] as? Date,
            updatedAt = map[AggregateRoot.KEY_UPDATED_AT] as? Date
        )
    }
}
