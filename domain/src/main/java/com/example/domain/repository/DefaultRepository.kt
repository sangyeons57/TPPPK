package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.factory.context.DefaultRepositoryFactoryContext
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface Repository

interface DefaultRepository<Domain> : Repository where Domain : AggregateRoot {
    fun ensureCollection(collectionPath: CollectionPath)

    suspend fun save(entity: Domain): CustomResult<DocumentId, Exception>
    suspend fun create(domain: Domain): CustomResult<DocumentId, Exception>
    suspend fun update(id: DocumentId, data: Map<String, Any?>): CustomResult<DocumentId, Exception>
    suspend fun delete(id: DocumentId): CustomResult<Unit, Exception>

    suspend fun findById(
        id: DocumentId,
        source: Source = Source.DEFAULT
    ): CustomResult<Domain, Exception>

    suspend fun findAll(source: Source = Source.DEFAULT): CustomResult<List<Domain>, Exception>
    suspend fun findNByUpdatedAt(
        n: Long,
        updatedAt: Instant,
        direction: Query.Direction = Query.Direction.DESCENDING
    ): CustomResult<List<Domain>, Exception>

    fun observe(id: DocumentId): Flow<CustomResult<Domain, Exception>>
    fun observeAll(): Flow<CustomResult<List<Domain>, Exception>>
    fun observeNByUpdatedAt(
        n: Long,
        updatedAt: Instant,
        direction: Query.Direction = Query.Direction.DESCENDING
    ): Flow<CustomResult<List<Domain>, Exception>>
}
