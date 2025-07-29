package com.example.data_core.datasource.local

import com.example.domain.model.base.ProjectsWrapper
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface LocalProjectsWrapperDataSource {
    suspend fun getWrapperById(wrapperId: String): ProjectsWrapper?
    suspend fun getWrappersByUser(userId: String): List<ProjectsWrapper>
    suspend fun getWrapperByUserAndProject(userId: String, projectId: String): ProjectsWrapper?
    suspend fun getAllWrappers(): List<ProjectsWrapper>
    suspend fun saveWrapper(wrapper: ProjectsWrapper)
    suspend fun saveWrappers(wrappers: List<ProjectsWrapper>)
    suspend fun deleteWrapper(wrapperId: String)
    suspend fun getWrappersUpdatedAfter(timestamp: Instant): List<ProjectsWrapper>
    fun observeWrappersByUser(userId: String): Flow<List<ProjectsWrapper>>
    suspend fun addToOutbox(wrapperId: String, operation: String, payload: String? = null)
    suspend fun getPendingOutboxOperations(): List<ProjectsWrapperOutboxOperation>
    suspend fun markOutboxOperationComplete(operationId: String)
    suspend fun incrementOutboxRetries(operationId: String)
    suspend fun getLastSyncCursor(): Long?
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long)
    suspend fun clearAllWrappers()
}

data class ProjectsWrapperOutboxOperation(
    val id: String,
    val wrapperId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)