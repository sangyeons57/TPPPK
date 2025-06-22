package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.special.Datasource
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.model.DTO
import com.example.data.model.remote.ScheduleDTO
import com.example.data.model.remote.toDto
import com.example.domain.event.AggregateRoot
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.base.Permission
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.RepositoryFactoryContext
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

abstract class DefaultRepositoryImpl  (
    private val defaultDatasource: DefaultDatasource,
    private val collectionPath: CollectionPath
): DefaultRepository  {

    init {
        defaultDatasource.setCollection(collectionPath.value)
    }


    override suspend fun delete(id: DocumentId): CustomResult<Unit, Exception> {
        return defaultDatasource.delete(id)
    }

    override suspend fun findById(id: DocumentId, source: Source): CustomResult<AggregateRoot, Exception> {
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
        return when(val result = defaultDatasource.findAll(source)) {
            is CustomResult.Success -> CustomResult.Success(result.data.map{ it.toDomain()})
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }

    override fun observe(id: DocumentId): Flow<CustomResult<AggregateRoot, Exception>> {
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