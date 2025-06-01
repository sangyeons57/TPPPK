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

    suspend fun getProjectDetails(projectId: String): CustomResult<Project, Exception>

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
}
