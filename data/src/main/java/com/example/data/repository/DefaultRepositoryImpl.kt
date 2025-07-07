package com.example.data.repository

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.DefaultRepositoryFactoryContext
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

abstract class DefaultRepositoryImpl  (
    private val defaultDatasource: DefaultDatasource,
    override val factoryContext: DefaultRepositoryFactoryContext
): DefaultRepository  {

    /**
     * Ensure the underlying [DefaultDatasource] is pointing to the latest collectionPath.
     * FactoryContext 구현체들은 run-time 에 `collectionPath` 를 변경할 수 있기 때문에
     * 각 public API 호출 시마다 현재 값을 다시 반영해 준다.
     */
    fun ensureCollection() {
        defaultDatasource.setCollection(factoryContext.collectionPath)
        Log.d("DefaultRepositoryImpl", "ensureCollection: ${factoryContext.collectionPath.value}")
    }

    fun ensureCollection(collectionPath: CollectionPath) {
        defaultDatasource.setCollection(collectionPath)
        Log.d("DefaultRepositoryImpl", "ensureCollection(Temp): ${collectionPath.value}")

    }

    override suspend fun delete(id: DocumentId): CustomResult<Unit, Exception> {
        ensureCollection()
        return defaultDatasource.delete(id)
    }

    override suspend fun findById(id: DocumentId, source: Source): CustomResult<AggregateRoot, Exception> {
        ensureCollection()
        return when (val result = defaultDatasource.findById(id)) {
            is CustomResult.Success -> CustomResult.Success(result.data.toDomain())
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }

    override suspend fun findAll(
        source: Source
    ): CustomResult<List<AggregateRoot>, Exception> {
        ensureCollection()
        return when(val result = defaultDatasource.findAll(source)) {
            is CustomResult.Success -> CustomResult.Success(result.data.map{ it.toDomain()})
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }

    override fun observe(id: DocumentId): Flow<CustomResult<AggregateRoot, Exception>> {
        ensureCollection()
        return defaultDatasource.observe(id).map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.toDomain())
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }

    override fun observeAll(): Flow<CustomResult<List<AggregateRoot>, Exception>> {
        ensureCollection()
        return defaultDatasource.observeAll()
            .map { dtoListResult ->
                when (dtoListResult) {
                    is CustomResult.Success -> CustomResult.Success(dtoListResult.data.map { it.toDomain()})
                    is CustomResult.Failure -> CustomResult.Failure(dtoListResult.error)
                    is CustomResult.Initial -> CustomResult.Initial
                    is CustomResult.Loading -> CustomResult.Loading
                    is CustomResult.Progress -> CustomResult.Progress(dtoListResult.progress)
                }
            }
    }
}