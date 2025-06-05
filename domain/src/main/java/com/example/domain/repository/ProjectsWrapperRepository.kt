package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectsWrapper
import kotlinx.coroutines.flow.Flow

interface ProjectsWrapperRepository {
    /**
     * 현재 사용자가 참여하고 있는 프로젝트의 ID를 담은 ProjectsWrapper 목록을 실시간으로 관찰합니다.
     * ProjectsWrapper는 projectId 필드만 가집니다.
     * @param userId 사용자 ID
     * @return ProjectsWrapper(projectId만 포함) 목록을 담은 Flow
     */
    fun observeProjectsWrappers(userId: String): Flow<List<ProjectsWrapper>>


    /**
     * 사용자의 프로젝트 참여 목록에 새 프로젝트를 추가합니다. (ProjectWrapper 생성)
     *
     * @param userId 사용자 ID
     * @param projectId 추가할 프로젝트 ID
     * @return 작업 성공/실패를 나타내는 CustomResult
     */
    suspend fun addProjectToUser(
        userId: String,
        projectId: String
    ): CustomResult<Unit, Exception>

    /**
     * 사용자의 프로젝트 참여 목록에서 특정 프로젝트를 제거합니다. (ProjectWrapper 삭제)
     *
     * @param userId 사용자 ID
     * @param projectId 제거할 프로젝트 ID
     * @return 작업 성공/실패를 나타내는 CustomResult
     */
    suspend fun removeProjectFromUser(userId: String, projectId: String): CustomResult<Unit, Exception>
    
    // ProjectsWrapper는 이제 프로젝트 참여 여부를 나타내는 projectId 목록 관리용으로 사용됩니다.
    // 실제 프로젝트 상세 정보(이름, 이미지 URL 등)는 ProjectRepository를 통해 조회해야 합니다.
}
