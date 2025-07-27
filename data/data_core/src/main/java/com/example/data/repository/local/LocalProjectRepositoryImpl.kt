package com.example.data.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data.datasource.local.LocalProjectsDataSource
import com.example.domain.model.base.Project
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.project.ProjectStatus
import com.example.domain.repository.local.LocalProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Project Repository Implementation (SSOT)
 * Room Database 전용 구현체 - UI에 직접 데이터 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - LocalDataSource를 통한 Room DB 접근
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 */
@Singleton
class LocalProjectRepositoryImpl @Inject constructor(
    private val localProjectsDataSource: LocalProjectsDataSource
) : LocalProjectRepository {

    companion object {
        private const val TAG = "LocalProjectRepository"
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeProjectById(projectId: String): Flow<Project?> {
        Log.d(TAG, "observeProjectById: $projectId")
        return localProjectsDataSource.observeProjectById(projectId)
    }

    override fun observeByName(name: ProjectName): Flow<Project?> {
        Log.d(TAG, "observeByName: ${name.value}")
        return localProjectsDataSource.observeAllProjects()
            .map { projects -> projects.find { it.name == name } }
    }

    override fun observeAllByName(name: String, limit: Int): Flow<List<Project>> {
        Log.d(TAG, "observeAllByName: name='$name', limit=$limit")
        return localProjectsDataSource.observeAllProjects()
            .map { projects ->
                projects.filter { it.name.value.contains(name, ignoreCase = true) }
                    .take(limit)
            }
    }

    override fun observeProjectsByOwner(ownerId: String): Flow<List<Project>> {
        Log.d(TAG, "observeProjectsByOwner: $ownerId")
        return localProjectsDataSource.observeProjectsByOwner(ownerId)
    }

    override fun observeProjectsByStatus(status: ProjectStatus): Flow<List<Project>> {
        Log.d(TAG, "observeProjectsByStatus: $status")
        return localProjectsDataSource.observeAllProjects()
            .map { projects -> projects.filter { it.status == status } }
    }

    override fun observeProjects(projectIds: List<String>): Flow<List<Project>> {
        Log.d(TAG, "observeProjects: ${projectIds.size} projects")
        return localProjectsDataSource.observeAllProjects()
            .map { projects -> projects.filter { it.id.value in projectIds } }
    }

    override fun observeProjectUpdatedAt(projectId: String): Flow<Long?> {
        Log.d(TAG, "observeProjectUpdatedAt: $projectId")
        return localProjectsDataSource.observeProjectById(projectId)
            .map { project -> project?.updatedAt?.toEpochMilli() }
    }

    override fun observeAllProjects(): Flow<List<Project>> {
        Log.d(TAG, "observeAllProjects")
        return localProjectsDataSource.observeAllProjects()
    }

    override fun observeActiveProjects(): Flow<List<Project>> {
        Log.d(TAG, "observeActiveProjects")
        return localProjectsDataSource.observeAllProjects()
            .map { projects -> projects.filter { it.status == ProjectStatus.ACTIVE } }
    }

    override fun observeProjectsByMember(userId: String): Flow<List<Project>> {
        Log.d(TAG, "observeProjectsByMember: $userId")
        // 현재 DataSource에 해당 메서드가 없으므로 전체 프로젝트에서 필터링
        return localProjectsDataSource.observeAllProjects()
            .map { projects ->
                // 실제 구현에서는 Members 테이블과 조인이 필요하지만 
                // 현재는 간단히 소유자 기준으로 필터링
                projects.filter { it.ownerId.value == userId }
            }
    }

    // === 단순 읽기 작업 ===

    override suspend fun getProjectById(projectId: String): Project? {
        Log.d(TAG, "getProjectById: $projectId")
        return try {
            localProjectsDataSource.getProjectById(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "getProjectById failed", e)
            null
        }
    }

    override suspend fun getProjectByName(name: ProjectName): Project? {
        Log.d(TAG, "getProjectByName: ${name.value}")
        return try {
            val projects = localProjectsDataSource.getAllProjects()
            projects.find { it.name == name }
        } catch (e: Exception) {
            Log.e(TAG, "getProjectByName failed", e)
            null
        }
    }

    override suspend fun searchProjectsByName(name: String, limit: Int): List<Project> {
        Log.d(TAG, "searchProjectsByName: name='$name', limit=$limit")
        return try {
            localProjectsDataSource.searchProjectsByName(name).take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "searchProjectsByName failed", e)
            emptyList()
        }
    }

    override suspend fun getProjectsByIds(projectIds: List<String>): List<Project> {
        Log.d(TAG, "getProjectsByIds: ${projectIds.size} projects")
        return try {
            val projects = mutableListOf<Project>()
            for (id in projectIds) {
                getProjectById(id)?.let { projects.add(it) }
            }
            projects
        } catch (e: Exception) {
            Log.e(TAG, "getProjectsByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getAllProjects(limit: Int?): List<Project> {
        Log.d(TAG, "getAllProjects: limit=$limit")
        return try {
            val projects = localProjectsDataSource.getAllProjects()
            if (limit != null) projects.take(limit) else projects
        } catch (e: Exception) {
            Log.e(TAG, "getAllProjects failed", e)
            emptyList()
        }
    }

    override suspend fun getProjectsByOwner(ownerId: String): List<Project> {
        Log.d(TAG, "getProjectsByOwner: $ownerId")
        return try {
            localProjectsDataSource.getProjectsByOwner(ownerId)
        } catch (e: Exception) {
            Log.e(TAG, "getProjectsByOwner failed", e)
            emptyList()
        }
    }

    override suspend fun getProjectsByStatus(status: ProjectStatus): List<Project> {
        Log.d(TAG, "getProjectsByStatus: $status")
        return try {
            localProjectsDataSource.getProjectsByStatus(status.name)
        } catch (e: Exception) {
            Log.e(TAG, "getProjectsByStatus failed", e)
            emptyList()
        }
    }

    override suspend fun getActiveProjects(): List<Project> {
        Log.d(TAG, "getActiveProjects")
        return try {
            localProjectsDataSource.getProjectsByStatus(ProjectStatus.ACTIVE.name)
        } catch (e: Exception) {
            Log.e(TAG, "getActiveProjects failed", e)
            emptyList()
        }
    }

    override suspend fun getProjectsByMember(userId: String): List<Project> {
        Log.d(TAG, "getProjectsByMember: $userId")
        return try {
            // 현재는 소유자 기준으로만 필터링
            // 실제 구현에서는 Members 테이블과 조인 필요
            localProjectsDataSource.getProjectsByOwner(userId)
        } catch (e: Exception) {
            Log.e(TAG, "getProjectsByMember failed", e)
            emptyList()
        }
    }

    // === 쓰기 작업 (Outbox 포함) ===

    override suspend fun saveProject(project: Project): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveProject: ${project.id}")

            // 1. Room DB에 저장
            localProjectsDataSource.saveProject(project)

            // 2. Outbox에 동기화 작업 추가
            val operation = if (project.isNew) "CREATE" else "UPDATE"
            localProjectsDataSource.addToOutbox(
                projectId = project.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            Log.d(TAG, "Project saved and added to outbox: ${project.id}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveProject failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun saveProjects(projects: List<Project>): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "saveProjects: ${projects.size} projects")

            if (projects.isEmpty()) {
                return CustomResult.Success(Unit)
            }

            // 대량 저장 (동기화용 - Outbox 추가 안 함)
            localProjectsDataSource.saveProjects(projects)

            Log.d(TAG, "Bulk projects saved: ${projects.size}")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "saveProjects failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun deleteProject(projectId: String): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "deleteProject: $projectId")

            // 1. Room DB에서 삭제 (실제로는 soft delete)
            localProjectsDataSource.deleteProject(projectId)

            // 2. Outbox에 삭제 작업 추가
            localProjectsDataSource.addToOutbox(
                projectId = projectId,
                operation = "DELETE",
                payload = null
            )

            Log.d(TAG, "Project deleted and added to outbox: $projectId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteProject failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun updateProject(
        projectId: String,
        name: ProjectName?,
        status: ProjectStatus?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "updateProject: projectId=$projectId, name=$name, status=$status")

            // 1. 현재 프로젝트 조회
            val currentProject = localProjectsDataSource.getProjectById(projectId)
                ?: return CustomResult.Failure(IllegalArgumentException("Project not found: $projectId"))

            // 2. 업데이트된 프로젝트 생성 (필요한 필드만 수정)
            var updatedProject = currentProject

            name?.let {
                updatedProject.changeName(it)
            }

            status?.let {
                when (it) {
                    ProjectStatus.ACTIVE -> updatedProject.activate()
                    ProjectStatus.ARCHIVED -> updatedProject.archive()
                    ProjectStatus.DELETED -> updatedProject.delete()
                }
            }

            // 3. 저장 (Outbox 포함)
            saveProject(updatedProject)

            Log.d(TAG, "Project updated: $projectId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "updateProject failed", e)
            CustomResult.Failure(e)
        }
    }

    override suspend fun changeProjectStatus(
        projectId: String,
        newStatus: ProjectStatus
    ): CustomResult<Unit, Exception> {
        return updateProject(projectId, null, newStatus)
    }

    override suspend fun changeProjectName(
        projectId: String,
        newName: ProjectName
    ): CustomResult<Unit, Exception> {
        return updateProject(projectId, newName, null)
    }

    // === 유틸리티 ===

    override suspend fun projectExists(projectId: String): Boolean {
        return try {
            localProjectsDataSource.projectExists(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "projectExists failed", e)
            false
        }
    }

    override suspend fun nameExists(name: ProjectName, excludeProjectId: String?): Boolean {
        return try {
            val projects = localProjectsDataSource.getAllProjects()
            projects.any { project ->
                project.name == name && (excludeProjectId == null || project.id.value != excludeProjectId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "nameExists failed", e)
            false
        }
    }

    override suspend fun getTotalProjectCount(): Int {
        return try {
            localProjectsDataSource.getProjectCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalProjectCount failed", e)
            0
        }
    }

    override suspend fun getProjectCountByOwner(ownerId: String): Int {
        return try {
            localProjectsDataSource.getProjectCountByOwner(ownerId)
        } catch (e: Exception) {
            Log.e(TAG, "getProjectCountByOwner failed", e)
            0
        }
    }

    override suspend fun getProjectCountByStatus(status: ProjectStatus): Int {
        return try {
            val projects = localProjectsDataSource.getProjectsByStatus(status.name)
            projects.size
        } catch (e: Exception) {
            Log.e(TAG, "getProjectCountByStatus failed", e)
            0
        }
    }

    override suspend fun getActiveProjectCount(): Int {
        return try {
            val activeProjects =
                localProjectsDataSource.getProjectsByStatus(ProjectStatus.ACTIVE.name)
            activeProjects.size
        } catch (e: Exception) {
            Log.e(TAG, "getActiveProjectCount failed", e)
            0
        }
    }

    override suspend fun clearAllProjects(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllProjects")

            localProjectsDataSource.clearAllProjects()

            Log.d(TAG, "All projects cleared")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "clearAllProjects failed", e)
            CustomResult.Failure(e)
        }
    }

    // === 동기화 지원 ===

    override suspend fun getProjectsUpdatedAfter(timestamp: Instant): List<Project> {
        return try {
            localProjectsDataSource.getProjectsUpdatedAfter(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "getProjectsUpdatedAfter failed", e)
            emptyList()
        }
    }

    override suspend fun addToOutbox(
        projectId: String,
        operation: String,
        payload: String?
    ): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "addToOutbox: projectId=$projectId, operation=$operation")

            localProjectsDataSource.addToOutbox(projectId, operation, payload)

            Log.d(TAG, "Added to outbox: $projectId")
            CustomResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "addToOutbox failed", e)
            CustomResult.Failure(e)
        }
    }
}