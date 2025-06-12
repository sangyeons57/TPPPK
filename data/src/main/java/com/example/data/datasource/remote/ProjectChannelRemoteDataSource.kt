
package com.example.data.datasource.remote

import com.example.data.model.remote.ProjectChannelDTO
import com.example.core_common.result.CustomResult // Added import
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
    ): CustomResult<String, Exception>

    /**
     * 프로젝트 채널의 정보를 수정합니다. (이름, 순서 등)
     * @param projectId 대상 프로젝트의 ID
     * @param channelDto 수정할 채널의 데이터가 담긴 DTO. DTO에는 채널 ID가 포함되어야 합니다.
     */
    suspend fun updateProjectChannel(
        projectId: String,
        channelDto: ProjectChannelDTO // Changed to accept DTO
    ): CustomResult<Unit, Exception> // Changed return type

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
    ): CustomResult<Unit, Exception>
}

