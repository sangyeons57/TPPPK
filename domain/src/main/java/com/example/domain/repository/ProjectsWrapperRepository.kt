package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectsWrapper
import kotlinx.coroutines.flow.Flow

interface ProjectsWrapperRepository {
    /**
     * 현재 사용자가 참여하고 있는 모든 프로젝트 목록을 ProjectsWrapper 형태로 스트림으로 가져옵니다.
     */
    fun getProjectsWrapperStream(userId: String): Flow<List<CustomResult<ProjectsWrapper, Exception>>>


    /**
     * Adds a project wrapper to the specified user's collection.
     *
     * @param userId The ID of the user.
     * @param projectId The ID of the project.
     * @param projectsWrapper The project wrapper domain model to add.
     * @return A [CustomResult] indicating success or failure.
     */
    suspend fun addProjectToUser(
        userId: String,
        projectId: String,
        projectsWrapper: ProjectsWrapper
    ): CustomResult<Unit, Exception>

    // TODO: ProjectsWrapper의 구체적인 사용 목적에 따라 필요한 함수 추가.
    // 만약 단순히 Project 목록만 필요하다면 ProjectRepository 사용을 우선적으로 고려하세요.
}
