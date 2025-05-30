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
    ): CustomResult<Project, Exception>

    suspend fun getProjectDetails(projectId: String): CustomResult<Project, Exception>
    fun getUserProjectsStream(userId: String): Flow<CustomResult<List<Project>, Exception>>
    suspend fun getPublicProjects(): CustomResult<List<Project>, Exception>

    suspend fun updateProjectInfo(
        projectId: String,
        name: String?,
        description: String?,
        isPublic: Boolean?
    ): CustomResult<Unit, Exception>

    suspend fun updateProjectImage(
        projectId: String,
        projectImageInputStream: InputStream,
        imageMimeType: String
    ): CustomResult<String, Exception>

    suspend fun deleteProject(projectId: String, currentUserId: String): CustomResult<Unit, Exception>
    suspend fun checkProjectNameAvailability(projectName: String): CustomResult<Boolean, Exception>
    suspend fun addMemberToProject(projectId: String, userId: String, roleId: String): CustomResult<Member, Exception>
    suspend fun removeMemberFromProject(projectId: String, userId: String, currentUserId: String): CustomResult<Unit, Exception>
    suspend fun getProjectStructure(projectId: String): Flow<CustomResult<List<Category>, Exception>>
    suspend fun updateProjectStructure(projectId: String, newStructure: List<Category>, currentUserId: String): CustomResult<Unit, Exception>
}
