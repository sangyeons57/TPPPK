package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.event.AggregateRoot
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
) : DefaultRepositoryImpl(categoryRemoteDataSource, factoryContext.collectionPath), CategoryRepository {

    /**
     * 카테고리 목록을 스트림으로 가져옵니다.
     * Firebase의 자체 캐싱 시스템을 활용하여 실시간 업데이트를 처리합니다.
     */
    override suspend fun getCategoriesStream(projectId: String): Flow<CustomResult<List<Category>, Exception>> {
        return categoryRemoteDataSource.observeCategories(projectId).map { dtoResultList ->
            if (dtoResultList is CustomResult.Success) {
                CustomResult.Success(dtoResultList.data.map { it.toDomain() })
            } else if (dtoResultList is CustomResult.Failure) {
                CustomResult.Failure(dtoResultList.error)
            } else {
                CustomResult.Failure(Exception("Unknown error getting categories"))
            }
        }
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Category)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Category"))

        return if (entity.id.isAssigned()) {
            categoryRemoteDataSource.update(entity.id, entity.getChangedFields())
        } else {
            categoryRemoteDataSource.create(entity.toDto())
        }
    }

    override suspend fun create(id: DocumentId, entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Category)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Category"))
        return categoryRemoteDataSource.create(entity.toDto())
    }
}
