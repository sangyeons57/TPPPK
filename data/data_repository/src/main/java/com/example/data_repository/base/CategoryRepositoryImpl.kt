package com.example.data_repository.base

import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.CategoryRemoteDataSource
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain_repository.base.CategoryRepository
import com.example.mapper.category.CategoryMapper
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryRemoteDataSource: CategoryRemoteDataSource,
    private val categoryMapper: CategoryMapper,
) : DefaultRepositoryImpl(categoryRemoteDataSource), CategoryRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Category)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Category"))
        ensureCollection()
        return if (entity.isNew) {
            categoryRemoteDataSource.create(categoryMapper.domainToDto(entity))
        } else {
            categoryRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
