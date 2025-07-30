package com.example.domain_repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.DocumentId
import com.example.domain.vo.CollectionPath
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow

interface Repository

interface DefaultRepository : Repository {
    fun setCollection(collectionPath: CollectionPath)

    suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception>
    suspend fun delete(id: DocumentId): CustomResult<Unit, Exception>

    suspend fun findById(id: DocumentId, source: Source = Source.DEFAULT): CustomResult<AggregateRoot, Exception>
    suspend fun findAll(source: Source = Source.DEFAULT): CustomResult<List<AggregateRoot>, Exception>

    fun observe(id: DocumentId): Flow<CustomResult<AggregateRoot, Exception>>
    fun observeAll(): Flow<CustomResult<List<AggregateRoot>, Exception>>
}
