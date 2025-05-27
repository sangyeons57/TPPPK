
package com.example.data.datasource._remote

import com.example.data.model._remote.ProjectChannelDTO
import kotlinx.coroutines.flow.Flow

interface ProjectChannelRemoteDataSource {

    /**
     * 특정 카테고리에 속한 모든 채널 목록을 실시간으로 관찰합니다.
     * @param projectId 대상 프로젝트의 ID
     * @param categoryId 채널 목록을 가져올 카테고리의 ID
     */
    fun observeProjectChannels(projectId: String, categoryId: String): Flow<List<ProjectChannelDTO>>

    /**
     * 카테고리에 새로운 채널을 추가합니다.
     * @param projectId 대상 프로젝트의 ID
     * @param categoryId 채널을 추가할 카테고리의 ID
     * @param name 새로운 채널의 이름
     * @param type 새로운 채널의 타입
     * @return 생성된 채널의 ID를 포함한 Result 객체
     */
    suspend fun addProjectChannel(
        projectId: String,
        categoryId: String,
        name: String,
        type: String
    ): Result<String>

    /**
     * 프로젝트 채널의 이름을 수정합니다.
     * @param projectId 대상 프로젝트의 ID
     * @param categoryId 대상 카테고리의 ID
     * @param channelId 수정할 채널의 ID
     * @param newName 새로운 채널의 이름
     */
    suspend fun updateProjectChannel(
        projectId: String,
        categoryId: String,
        channelId: String,
        newName: String
    ): Result<Unit>

    /**
     * 프로젝트 채널을 삭제합니다.
     * @param projectId 대상 프로젝트의 ID
     * @param categoryId 대상 카테고리의 ID
     * @param channelId 삭제할 채널의 ID
     */
    suspend fun deleteProjectChannel(
        projectId: String,
        categoryId: String,
        channelId: String
    ): Result<Unit>
}

