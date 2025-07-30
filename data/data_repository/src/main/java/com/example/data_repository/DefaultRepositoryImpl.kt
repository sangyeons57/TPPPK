package com.example.data_repository

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.special.DefaultDatasource
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.DocumentId
import com.example.domain.vo.CollectionPath
import com.example.domain_repository.DefaultRepository
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

abstract class DefaultRepositoryImpl  (
    private val defaultDatasource: DefaultDatasource,
): DefaultRepository  {

    private var currentCollectionPath: CollectionPath? = null

    override fun setCollection(collectionPath: CollectionPath) {
        currentCollectionPath = collectionPath
        Log.d("DefaultRepositoryImpl", "setCollection: ${collectionPath.value}")
    }

    /**
     * Ensure the underlying DefaultDatasource is pointing to the current collectionPath.
     */
    protected fun ensureCollection() {
        currentCollectionPath?.let { collectionPath ->
            defaultDatasource.setCollection(collectionPath)
            Log.d("DefaultRepositoryImpl", "ensureCollection: ${collectionPath.value}")
        } ?: throw IllegalStateException("Collection path not set. Call setCollection() first.")
    }

    override suspend fun delete(id: DocumentId): CustomResult<Unit, Exception> {
        ensureCollection()
        return defaultDatasource.delete(id)
    }

    override suspend fun findById(id: DocumentId, source: Source): CustomResult<AggregateRoot, Exception> {
        ensureCollection()
        Log.d("DefaultRepositoryImpl", "findById: documentId=${id.value}, source=$source")
        return when (val result = defaultDatasource.findById(id, source)) {
            is CustomResult.Success -> {
                Log.d("DefaultRepositoryImpl", "findById success: documentId=${id.value}")
                // TODO: This should be overridden by concrete implementations with proper mapping
                throw NotImplementedError("findById should be overridden by concrete repository implementations")
            }
            is CustomResult.Failure -> {
                Log.e("DefaultRepositoryImpl", "findById failed: documentId=${id.value}, error=${result.error.message}", result.error)
                CustomResult.Failure(result.error)
            }
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
            is CustomResult.Success -> {
                // TODO: This should be overridden by concrete implementations with proper mapping
                throw NotImplementedError("findAll should be overridden by concrete repository implementations")
            }
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
                is CustomResult.Success -> {
                    // TODO: This should be overridden by concrete implementations with proper mapping
                    throw NotImplementedError("observe should be overridden by concrete repository implementations")
                }
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
                    is CustomResult.Success -> {
                        // TODO: This should be overridden by concrete implementations with proper mapping
                        throw NotImplementedError("observeAll should be overridden by concrete repository implementations")
                    }
                    is CustomResult.Failure -> CustomResult.Failure(dtoListResult.error)
                    is CustomResult.Initial -> CustomResult.Initial
                    is CustomResult.Loading -> CustomResult.Loading
                    is CustomResult.Progress -> CustomResult.Progress(dtoListResult.progress)
                }
            }
    }
}