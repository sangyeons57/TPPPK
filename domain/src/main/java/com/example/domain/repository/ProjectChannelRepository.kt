package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import kotlinx.coroutines.flow.Flow

interface ProjectChannelRepository {
    fun getProjectChannelsStream(projectId: String, categoryId: String): Flow<CustomResult<List<ProjectChannel>, Exception>>
    suspend fun addProjectChannel(projectId: String, channel: ProjectChannel): CustomResult<Unit, Exception>
    suspend fun setProjectChannel(projectId: String, categoryId: String, channel: ProjectChannel): CustomResult<Unit, Exception>
    suspend fun updateProjectChannel(projectId: String, channel: ProjectChannel): CustomResult<Unit, Exception>
    suspend fun deleteProjectChannel(projectId: String, channelId: String): CustomResult<Unit, Exception>
}
