
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.model.remote.ProjectDTO
import kotlinx.coroutines.flow.Flow

interface ProjectRemoteDataSource {

    /**
     * 특정 프로젝트의 정보를 실시간으로 관찰합니다.
     * @param projectId 관찰할 프로젝트의 ID
     */
    fun observeProject(projectId: String): Flow<ProjectDTO?>

    /**
     * 특정 프로젝트의 정보를 한 번 가져옵니다.
     * @param projectId 조회할 프로젝트의 ID
     */
    suspend fun getProject(projectId: String): CustomResult<ProjectDTO, Exception>

    /**
     * 새로운 프로젝트를 생성합니다.
     * @param name 생성할 프로젝트의 이름
     * @param isPublic 공개 여부
     * @return 생성된 프로젝트의 ID를 포함한 Result 객체
     */
    suspend fun createProject(name: String, isPublic: Boolean): CustomResult<String, Exception>

    /**
     * 프로젝트의 이름과 이미지 URL을 업데이트합니다.
     * @param projectId 업데이트할 프로젝트의 ID
     * @param name 새로운 이름
     * @param imageUrl 새로운 이미지 URL
     */
    suspend fun updateProjectDetails(
        projectId: String,
        name: String,
        imageUrl: String?
    ): CustomResult<Unit, Exception>

    /**
     * 프로젝트를 삭제합니다.
     * @param projectId 삭제할 프로젝트의 ID
     */
    suspend fun deleteProject(projectId: String): CustomResult<Unit, Exception>
}

