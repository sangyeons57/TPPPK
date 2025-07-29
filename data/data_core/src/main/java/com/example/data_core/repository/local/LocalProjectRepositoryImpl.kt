package com.example.data_core.repository.local

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.data_core.dao.ProjectsDao
import com.example.data_core.repository.local.base.BaseLocalRepositoryImpl
import com.example.domain.model.base.Project
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.project.ProjectStatus
import com.example.domain.repository.infrastructure.OutboxRepository
import com.example.domain.repository.local.LocalProjectRepository
import com.example.mapper.ProjectEntityMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Project Repository Implementation (SSOT)
 * BaseLocalRepositoryImpl 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지
 *
 * ✅ 역할:
 * - BaseLocalRepositoryImpl의 공통 CRUD 기능 상속 (80%)
 * - Project 도메인 특화 기능만 구현 (20%)
 * - Room DB 직접 접근 (DAO 사용)
 * - Flow로 UI에 실시간 데이터 제공
 * - 로컬 CRUD 작업 처리
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 메서드 구현:
 * - observeEntityById -> observeProjectById로 위임
 * - observeAllEntities -> observeAllProjects로 위임
 * - observeEntityUpdatedAt -> observeProjectUpdatedAt로 위임
 * - getEntityById -> getProjectById로 위임
 * - getEntitiesByIds -> getProjectsByIds로 위임
 * - getAllEntities -> getAllProjects로 위임
 * - saveEntity -> saveProject로 위임
 * - saveEntities -> saveProjects로 위임
 * - deleteEntity -> deleteProject로 위임
 * - Plus SyncableRepository methods
 */
@Singleton
class LocalProjectRepositoryImpl @Inject constructor(
    private val projectsDao: ProjectsDao,
    private val outboxRepository: OutboxRepository,
    private val mapper: ProjectEntityMapper
) : BaseLocalRepositoryImpl<Project>(), LocalProjectRepository {

    companion object {
        private const val TAG = "LocalProjectRepository"
        private const val COLLECTION_NAME = "projects"
    }

    // === BaseLocalRepository 메서드 구현 (도메인 특화 메서드로 위임) ===

    override fun observeEntityById(entityId: String): Flow<Project?> = 
        observeProjectById(entityId)

    override fun observeAllEntities(): Flow<List<Project>> = 
        observeAllProjects()

    override fun observeEntityUpdatedAt(entityId: String): Flow<Long?> = 
        observeProjectUpdatedAt(entityId)

    override suspend fun getEntityById(entityId: String): CustomResult<Project?, Exception> = 
        handleOperation("getProjectById($entityId)", TAG) {
            getProjectById(entityId)
        }

    override suspend fun getEntitiesByIds(entityIds: List<String>): CustomResult<List<Project>, Exception> = 
        handleOperation("getProjectsByIds(${entityIds.size})", TAG) {
            getProjectsByIds(entityIds)
        }

    override suspend fun getAllEntities(limit: Int?): CustomResult<List<Project>, Exception> = 
        handleOperation("getAllProjects($limit)", TAG) {
            getAllProjects(limit)
        }

    override suspend fun saveEntity(entity: Project): CustomResult<Unit, Exception> = 
        saveProject(entity)

    override suspend fun saveEntities(entities: List<Project>): CustomResult<Unit, Exception> = 
        saveProjects(entities)

    override suspend fun deleteEntity(entityId: String): CustomResult<Unit, Exception> = 
        deleteProject(entityId)

    override suspend fun getEntitiesUpdatedAfter(timestamp: Instant): CustomResult<List<Project>, Exception> = 
        handleOperation("getProjectsUpdatedAfter($timestamp)", TAG) {
            getProjectsUpdatedAfter(timestamp)
        }

    override suspend fun clearAllEntities(): CustomResult<Unit, Exception> = 
        clearAllProjects()

    // === BaseLocalRepositoryImpl 추상 메서드 구현 ===

    override suspend fun getTotalEntityCountInternal(): Int {
        return getTotalProjectCount()
    }

    override suspend fun entityExistsInternal(entityId: String): Boolean {
        return projectExists(entityId)
    }

    // === 관찰자 패턴 (UI 반응형) ===

    override fun observeProjectById(projectId: String): Flow<Project?> {
        Log.d(TAG, "observeProjectById: $projectId")
        return projectsDao.observeProjectById(projectId).map { it?.let { mapper.toDomain(it) } }
    }

    override fun observeByName(name: ProjectName): Flow<Project?> {
        Log.d(TAG, "observeByName: ${name.value}")
        return projectsDao.observeAllProjects()
            .map { projects -> projects.find { it.name == name.value }?.let { mapper.toDomain(it) } }
    }

    override fun observeAllByName(name: String, limit: Int): Flow<List<Project>> {
        Log.d(TAG, "observeAllByName: name='$name', limit=$limit")
        return projectsDao.searchProjectsByName("%$name%")
            .map { entities -> entities.map { mapper.toDomain(it) } }
    }

    override fun observeProjectsByOwner(ownerId: String): Flow<List<Project>> {
        Log.d(TAG, "observeProjectsByOwner: $ownerId")
        return projectsDao.observeProjectsByOwner(ownerId).map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeProjectsByStatus(status: ProjectStatus): Flow<List<Project>> {
        Log.d(TAG, "observeProjectsByStatus: $status")
        return projectsDao.observeProjectsByStatus(status.name)
            .map { entities -> entities.map { mapper.toDomain(it) } }
    }

    override fun observeProjects(projectIds: List<String>): Flow<List<Project>> {
        Log.d(TAG, "observeProjects: ${projectIds.size} projects")
        return projectsDao.observeAllProjects()
            .map { entities -> entities.filter { it.id in projectIds }.map { mapper.toDomain(it) } }
    }

    override fun observeProjectUpdatedAt(projectId: String): Flow<Long?> {
        Log.d(TAG, "observeProjectUpdatedAt: $projectId")
        return projectsDao.observeProjectById(projectId)
            .map { project -> project?.updatedAt?.toEpochMilli() }
    }

    override fun observeAllProjects(): Flow<List<Project>> {
        Log.d(TAG, "observeAllProjects")
        return projectsDao.observeAllProjects().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeActiveProjects(): Flow<List<Project>> {
        Log.d(TAG, "observeActiveProjects")
        return projectsDao.getActiveProjects().map { entities ->
            entities.map { mapper.toDomain(it) }
        }
    }

    override fun observeProjectsByMember(userId: String): Flow<List<Project>> {
        Log.d(TAG, "observeProjectsByMember: $userId")
        // 현재는 소유자 기준으로만 필터링
        // 실제 구현에서는 Members 테이블과 조인 필요
        return projectsDao.observeProjectsByOwner(userId)
            .map { entities -> entities.map { mapper.toDomain(it) } }
    }

    // === 단순 읽기 작업 ===

    override suspend fun getProjectById(projectId: String): Project? {
        Log.d(TAG, "getProjectById: $projectId")
        return try {
            projectsDao.getProjectById(projectId)?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getProjectById failed", e)
            null
        }
    }

    override suspend fun getProjectByName(name: ProjectName): Project? {
        Log.d(TAG, "getProjectByName: ${name.value}")
        return try {
            projectsDao.searchProjectsByName(name.value)
                .find { it.name == name.value }?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getProjectByName failed", e)
            null
        }
    }

    override suspend fun searchProjectsByName(name: String, limit: Int): List<Project> {
        Log.d(TAG, "searchProjectsByName: name='$name', limit=$limit")
        return try {
            projectsDao.searchProjectsByName(name).map { mapper.toDomain(it) }.take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "searchProjectsByName failed", e)
            emptyList()
        }
    }

    override suspend fun getProjectsByIds(projectIds: List<String>): List<Project> {
        Log.d(TAG, "getProjectsByIds: ${projectIds.size} projects")
        return try {
            projectsDao.getAllProjects().filter { it.id in projectIds }.map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getProjectsByIds failed", e)
            emptyList()
        }
    }

    override suspend fun getAllProjects(limit: Int?): List<Project> {
        Log.d(TAG, "getAllProjects: limit=$limit")
        return try {
            val projects = projectsDao.getAllProjects().map { mapper.toDomain(it) }
            if (limit != null) projects.take(limit) else projects
        } catch (e: Exception) {
            Log.e(TAG, "getAllProjects failed", e)
            emptyList()
        }
    }

    override suspend fun getProjectsByOwner(ownerId: String): List<Project> {
        Log.d(TAG, "getProjectsByOwner: $ownerId")
        return try {
            projectsDao.getProjectsByOwner(ownerId).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getProjectsByOwner failed", e)
            emptyList()
        }
    }

    override suspend fun getProjectsByStatus(status: ProjectStatus): List<Project> {
        Log.d(TAG, "getProjectsByStatus: $status")
        return try {
            projectsDao.getProjectsByStatus(status.name).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getProjectsByStatus failed", e)
            emptyList()
        }
    }

    override suspend fun getActiveProjects(): List<Project> {
        Log.d(TAG, "getActiveProjects")
        return try {
            projectsDao.getActiveProjects().map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getActiveProjects failed", e)
            emptyList()
        }
    }

    override suspend fun getProjectsByMember(userId: String): List<Project> {
        Log.d(TAG, "getProjectsByMember: $userId")
        return try {
            projectsDao.getProjectsByOwner(userId).map { mapper.toDomain(it) }
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
            projectsDao.insertProject(mapper.toEntity(project))

            // 2. OutboxRepository를 통한 동기화 작업 추가
            val operation = if (project.isNew) "CREATE" else "UPDATE"
            val outboxResult = outboxRepository.enqueue(
                collectionName = COLLECTION_NAME,
                documentId = project.id.value,
                operation = operation,
                payload = null // 필요시 JSON 직렬화된 변경사항
            )

            when (outboxResult) {
                is CustomResult.Success -> {
                    Log.d(TAG, "Project saved and added to outbox: ${project.id}")
                    CustomResult.Success(Unit)
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "Failed to add project to outbox", outboxResult.error)
                    // DB 저장은 성공했지만 Outbox 추가 실패 - 경고만 출력하고 성공 처리
                    CustomResult.Success(Unit)
                }
            }

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
            projectsDao.insertProjects(projects.map { mapper.toEntity(it) })

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
            projectsDao.deleteProject(projectId)

            // 2. OutboxRepository를 통한 삭제 작업 추가
            val outboxResult = outboxRepository.enqueue(
                collectionName = COLLECTION_NAME,
                documentId = projectId,
                operation = "DELETE",
                payload = null
            )

            when (outboxResult) {
                is CustomResult.Success -> {
                    Log.d(TAG, "Project deleted and added to outbox: $projectId")
                    CustomResult.Success(Unit)
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "Failed to add delete operation to outbox", outboxResult.error)
                    // DB 삭제는 성공했지만 Outbox 추가 실패 - 경고만 출력하고 성공 처리
                    CustomResult.Success(Unit)
                }
            }

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
            val currentProject = projectsDao.getProjectById(projectId)?.let { mapper.toDomain(it) }
                ?: return CustomResult.Failure(IllegalArgumentException("Project not found: $projectId"))

            // 2. 업데이트된 프로젝트 생성 (필요한 필드만 수정)
            var updatedProject = currentProject

            name?.let {
                updatedProject = updatedProject.changeName(it)
            }

            status?.let {
                updatedProject = when (it) {
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
            projectsDao.projectExists(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "projectExists failed", e)
            false
        }
    }

    override suspend fun nameExists(name: ProjectName, excludeProjectId: String?): Boolean {
        return try {
            projectsDao.searchProjectsByName(name.value)
                .any { it.name == name.value && it.id != excludeProjectId }
        } catch (e: Exception) {
            Log.e(TAG, "nameExists failed", e)
            false
        }
    }

    override suspend fun getTotalProjectCount(): Int {
        return try {
            projectsDao.getProjectCount()
        } catch (e: Exception) {
            Log.e(TAG, "getTotalProjectCount failed", e)
            0
        }
    }

    override suspend fun getProjectCountByOwner(ownerId: String): Int {
        return try {
            projectsDao.getProjectCountByOwner(ownerId)
        } catch (e: Exception) {
            Log.e(TAG, "getProjectCountByOwner failed", e)
            0
        }
    }

    override suspend fun getProjectCountByStatus(status: ProjectStatus): Int {
        return try {
            projectsDao.getProjectCountByStatus(status.name)
        } catch (e: Exception) {
            Log.e(TAG, "getProjectCountByStatus failed", e)
            0
        }
    }

    override suspend fun getActiveProjectCount(): Int {
        return try {
            projectsDao.getProjectsByStatus(ProjectStatus.ACTIVE.name)
                .size
        } catch (e: Exception) {
            Log.e(TAG, "getActiveProjectCount failed", e)
            0
        }
    }

    override suspend fun clearAllProjects(): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "clearAllProjects")

            projectsDao.deleteAllProjects()

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
            projectsDao.getProjectsUpdatedAfter(timestamp).map { mapper.toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getProjectsUpdatedAfter failed", e)
            emptyList()
        }
    }

}