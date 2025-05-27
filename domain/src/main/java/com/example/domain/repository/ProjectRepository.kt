package com.example.domain._repository

import com.example.domain.model.Project
import com.example.domain.model.ProjectMember
import com.example.domain.model.ProjectStructure
import java.io.InputStream
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 프로젝트 생성, 조회, 관리 등 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface ProjectRepository {
    suspend fun createProject(
        name: String,
        description: String?,
        ownerId: String,
        isPublic: Boolean,
        projectImageInputStream: InputStream?,
        imageMimeType: String?
    ): Result<Project>

    suspend fun getProjectDetails(projectId: String): Result<Project>
    fun getUserProjectsStream(userId: String): Flow<Result<List<Project>>>
    suspend fun getPublicProjects(): Result<List<Project>>

    suspend fun updateProjectInfo(
        projectId: String,
        name: String?,
        description: String?,
        isPublic: Boolean?
    ): Result<Unit>

    suspend fun updateProjectImage(
        projectId: String,
        projectImageInputStream: InputStream,
        imageMimeType: String
    ): Result<String?>

    suspend fun deleteProject(projectId: String, currentUserId: String): Result<Unit>
    suspend fun checkProjectNameAvailability(projectName: String): Result<Boolean>
    suspend fun addMemberToProject(projectId: String, userId: String, roleId: String): Result<ProjectMember>
    suspend fun removeMemberFromProject(projectId: String, userId: String, currentUserId: String): Result<Unit>
    suspend fun getProjectStructure(projectId: String): Result<ProjectStructure>
    suspend fun updateProjectStructure(projectId: String, newStructure: ProjectStructure, currentUserId: String): Result<Unit>
}
