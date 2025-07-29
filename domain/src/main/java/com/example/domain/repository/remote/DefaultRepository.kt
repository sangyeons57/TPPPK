package com.example.domain.repository.remote

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface Repository

interface DefaultRepository<Domain> : Repository where Domain : AggregateRoot {
    fun setCollection(collectionPath: CollectionPath)

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

    /**
     * 커서 기반 페이지네이션으로 데이터 조회
     * TODO: 구현 필요
     */
    suspend fun findNAfterCursor(
        n: Long,
        cursor: String,
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