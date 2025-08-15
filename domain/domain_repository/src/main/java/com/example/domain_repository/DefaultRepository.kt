package com.example.domain_repository

import com.example.core_common.result.CustomResult
import com.example.domain.AggregateRoot
import com.example.domain.vo.CollectionPath
import com.example.domain.vo.DocumentId
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow

interface Repository

interface DefaultRepository<D : AggregateRoot> : Repository {
    fun setCollection(collectionPath: CollectionPath)

    suspend fun save(entity: D): CustomResult<DocumentId, Exception>
    suspend fun delete(id: DocumentId): CustomResult<Unit, Exception>

    // 직접 필드 업데이트를 위한 메서드 추가
    suspend fun updateFields(
        id: DocumentId,
        fields: Map<String, Any?>
    ): CustomResult<DocumentId, Exception>

    suspend fun findById(
        id: DocumentId,
        source: Source = Source.DEFAULT
    ): CustomResult<D, Exception>

    suspend fun findAll(source: Source = Source.DEFAULT): CustomResult<List<D>, Exception>

    fun observe(id: DocumentId): Flow<CustomResult<D, Exception>>
    fun observeAll(): Flow<CustomResult<List<D>, Exception>>
}
