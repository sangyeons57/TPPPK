package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.event.AggregateRoot
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow

interface Repository {

}

interface DefaultRepository : Repository {
    val factoryContext: DefaultRepositoryFactoryContext

    suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception>
    suspend fun create(id: DocumentId, entity: AggregateRoot): CustomResult<DocumentId, Exception>
    suspend fun delete(id: DocumentId): CustomResult<Unit, Exception>
    suspend fun findById(id: DocumentId, source: Source = Source.DEFAULT): CustomResult<AggregateRoot, Exception>
    suspend fun findAll(source: Source = Source.DEFAULT): CustomResult<List<AggregateRoot>, Exception>
    fun observe(id: DocumentId): Flow<CustomResult<AggregateRoot, Exception>>
    fun observeAll(): Flow<CustomResult<List<AggregateRoot>, Exception>>

}
