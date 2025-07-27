package com.example.data.repository

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.model.DTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.DefaultRepositoryFactoryContext
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.reflect.typeOf

abstract class DefaultRepositoryImpl<Domain, Data>(
    private val defaultDatasource: DefaultDatasource<Data>,
    private val clazz: Class<Domain>,
) : DefaultRepository<Domain> where Data : DTO, Domain : AggregateRoot {

    override fun ensureCollection(collectionPath: CollectionPath) {
        defaultDatasource.setCollection(collectionPath)
        Log.d("DefaultRepositoryImpl", "ensureCollection(Temp): ${collectionPath.value}")

    }

    suspend fun create(domain: Domain): CustomResult<DocumentId, Exception> {
        return defaultDatasource.create(domain)
    }

    override suspend fun update(
        id: DocumentId,
        data: Map<String, Any?>
    ): CustomResult<DocumentId, Exception> {
        return defaultDatasource.update(id, data)
    }

    override suspend fun delete(id: DocumentId): CustomResult<Unit, Exception> {
        return defaultDatasource.delete(id)
    }

    override suspend fun findById(id: DocumentId, source: Source): CustomResult<Domain, Exception> {
        Log.d("DefaultRepositoryImpl", "findById: documentId=${id.value}, source=$source")
        return when (val result = defaultDatasource.findById(id, source)) {
            is CustomResult.Success -> {
                Log.d("DefaultRepositoryImpl", "findById success: documentId=${id.value}")
                resultTry { clazz.cast(result.data.toDomain()) }
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
    ): CustomResult<List<Domain>, Exception> {
        return when(val result = defaultDatasource.findAll(source)) {
            is CustomResult.Success -> resultTry { (result.data.map { clazz.cast(it.toDomain()) }) }
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }

    override fun observe(id: DocumentId): Flow<CustomResult<Domain, Exception>> {
        return defaultDatasource.observe(id).map { result ->
            when (result) {
                is CustomResult.Success -> {
                    result.data.let { resultTry { clazz.cast(it.toDomain())!! } }
                }
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }

    override fun observeAll(): Flow<CustomResult<List<Domain>, Exception>> {
        return defaultDatasource.observeAll()
            .map { dtoListResult ->
                when (dtoListResult) {
                    is CustomResult.Success -> resultTry {
                        dtoListResult.data.map { clazz.cast(it.toDomain())!! }.toList()
                    }
                    is CustomResult.Failure -> CustomResult.Failure(dtoListResult.error)
                    is CustomResult.Initial -> CustomResult.Initial
                    is CustomResult.Loading -> CustomResult.Loading
                    is CustomResult.Progress -> CustomResult.Progress(dtoListResult.progress)
                }
            }
    }
}