package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.event.EventDispatcher
import com.example.domain.model.base.Category
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.RepositoryFactoryContext
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.factory.context.CategoryRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryRemoteDataSource: CategoryRemoteDataSource,
    override val factoryContext: CategoryRepositoryFactoryContext,
) : DefaultRepositoryImpl(categoryRemoteDataSource, factoryContext), CategoryRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Category)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Category"))
        ensureCollection()
        return if (entity.isNew) {
            categoryRemoteDataSource.create(entity.toDto())
        } else {
            categoryRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
