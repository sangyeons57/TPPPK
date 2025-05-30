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
     * ProjectsWrapper 정보를 서버로부터 강제로 새로고침합니다.
     */
    suspend fun refreshProjectsWrapper(userId: String): CustomResult<Unit, Exception>

    // TODO: ProjectsWrapper의 구체적인 사용 목적에 따라 필요한 함수 추가.
    // 만약 단순히 Project 목록만 필요하다면 ProjectRepository 사용을 우선적으로 고려하세요.
}
