package com.example.domain_repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Task
import com.example.domain.vo.DocumentId
import com.example.domain_repository.DefaultRepository
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow

interface TaskRepository : DefaultRepository<Task> {
    suspend fun addTask(payload: Task)

    override suspend fun findById(id: DocumentId, source: Source): CustomResult<Task, Exception>

    /**
     * Observe tasks by projectId using Room as SSOT
     */
    fun observeByProject(projectId: String): Flow<CustomResult<List<Task>, Exception>>

    /**
     * Observe tasks by exact channelId using Room as SSOT
     */
    fun observeByChannel(channelId: String): Flow<CustomResult<List<Task>, Exception>>
}
