package com.example.mapper.entity

import com.example.data_model.local.CategoriesEntity
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.model.vo.category.IsCategoryFlag
import com.example.mapper.base.BaseEntityMapper
import javax.inject.Inject

interface CategoryEntityMapper : BaseEntityMapper<Category, CategoriesEntity> {
    fun toEntity(domain: Category, projectId: String): CategoriesEntity
}

class CategoryEntityMapperImpl @Inject constructor() : CategoryEntityMapper {
    override fun toDomain(entity: CategoriesEntity): Category {
        return Category.fromDataSource(
            id = DocumentId(entity.id),
            name = CategoryName(entity.name),
            order = CategoryOrder(entity.order),
            createdBy = OwnerId(entity.createdBy),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isCategory = IsCategoryFlag(entity.isCategory)
        )
    }

    override fun toEntity(domain: Category): CategoriesEntity {
        // This method is problematic because we don't have projectId here.
        // The overloaded method should be used instead.
        throw UnsupportedOperationException("Use toEntity(domain, projectId) instead")
    }

    override fun toEntity(domain: Category, projectId: String): CategoriesEntity {
        return CategoriesEntity(
            id = domain.id.value,
            projectId = projectId,
            name = domain.name.value,
            order = domain.order.value,
            createdBy = domain.createdBy.value,
            isCategory = domain.isCategory.value,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
