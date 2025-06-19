package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import kotlinx.coroutines.flow.Flow

interface DefaultRepository<T> {
    suspend fun save(entity: T): CustomResult<DocumentId, Exception>
    suspend fun delete(id: DocumentId): CustomResult<Unit, Exception>
    suspend fun findById(id: DocumentId): CustomResult<T, Exception>
    fun observe(id: DocumentId): Flow<CustomResult<T, Exception>>
}
