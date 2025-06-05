package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.base.Member
import com.example.domain.model.base.Project
import java.io.InputStream
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 생성, 조회, 관리 등 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface ProjectRepository {
    suspend fun createProject(
        name: String,
        ownerId: String,
    ): CustomResult<String, Exception>

    fun getProjectDetailsStream(projectId: String): Flow<CustomResult<Project, Exception>>

    suspend fun updateProjectInfo(
        projectId: String,
        name: String?,
    ): CustomResult<Unit, Exception>

    suspend fun updateProjectImage(
        projectId: String,
        projectImageInputStream: InputStream,
        imageMimeType: String
    ): CustomResult<String, Exception>

    suspend fun deleteProject(projectId: String, currentUserId: String): CustomResult<Unit, Exception>
    suspend fun getProjectStructureStream(projectId: String): Flow<CustomResult<List<Category>, Exception>>

    /**
     * 프로젝트의 프로필 이미지 URL만 업데이트합니다.
     * Firestore의 프로젝트 문서에 있는 imageUrl 필드를 직접 수정합니다.
     *
     * @param projectId 업데이트할 프로젝트의 ID.
     * @param imageUrl 새 프로필 이미지의 다운로드 URL. null일 경우 필드를 제거하거나 기본값으로 설정합니다.
     * @return 작업 성공 시 [CustomResult.Success] (Unit), 실패 시 [CustomResult.Failure] (Exception).
     */
    suspend fun updateProjectProfileImageUrl(projectId: String, imageUrl: String?): CustomResult<Unit, Exception>
}
