package com.example.domain.repository.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Project
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.project.ProjectStatus
import com.example.domain.repository.local.base.BaseLocalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Local Project Repository Interface (SSOT)
 * BaseLocalRepository 상속으로 공통 CRUD 기능 자동 제공
 *
 * 🔒 제약사항:
 * - 외부 네트워크 호출 절대 금지
 * - Firestore 직접 접근 금지 (Remote ProjectRepository 사용)
 *
 * ✅ 역할:
 * - BaseLocalRepository의 공통 CRUD 기능 상속 (80%)
 * - Project 도메인 특화 기능만 추가 정의 (20%)
 * - Flow로 UI에 실시간 데이터 제공 (Observer Pattern)
 * - Outbox 관리 (동기화 대상 저장)
 *
 * 📋 BaseLocalRepository 상속 메서드:
 * - observeEntityById -> observeProjectById
 * - observeAllEntities -> observeAllProjects
 * - observeEntityUpdatedAt -> observeProjectUpdatedAt
 * - getEntityById -> getProjectById
 * - getEntitiesByIds -> getProjectsByIds
 * - getAllEntities -> getAllProjects
 * - saveEntity -> saveProject
 * - saveEntities -> saveProjects
 * - deleteEntity -> deleteProject
 * - Plus SyncableRepository methods (addToOutbox, clearAllEntities, etc.)
 */
interface LocalProjectRepository : BaseLocalRepository<Project> {

    // === BaseLocalRepository 메서드 (구현체에서 프로젝트 전용 메서드로 매핑) ===
    // observeEntityById -> observeProjectById
    // observeAllEntities -> observeAllProjects  
    // observeEntityUpdatedAt -> observeProjectUpdatedAt
    // getEntityById -> getProjectById
    // getEntitiesByIds -> getProjectsByIds
    // getAllEntities -> getAllProjects
    // saveEntity -> saveProject
    // saveEntities -> saveProjects
    // deleteEntity -> deleteProject
    // getEntitiesUpdatedAfter -> getProjectsUpdatedAfter
    // clearAllEntities -> clearAllProjects
    // getTotalEntityCount -> getTotalProjectCount
    // entityExists -> projectExists

    // === 관찰자 패턴 (UI 반응형) ===

    /**
     * 특정 프로젝트를 실시간 관찰
     * @param projectId 프로젝트 ID
     * @return 프로젝트 Flow (null 가능)
     */
    fun observeProjectById(projectId: String): Flow<Project?>

    /**
     * 주어진 이름과 정확히 일치하는 프로젝트를 실시간 관찰
     * @param name 프로젝트 이름
     * @return 프로젝트 Flow
     */
    fun observeByName(name: ProjectName): Flow<Project?>

    /**
     * 주어진 이름을 포함하는 프로젝트 목록을 실시간 관찰
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 프로젝트 목록 Flow
     */
    fun observeAllByName(name: String, limit: Int = 10): Flow<List<Project>>

    /**
     * 특정 소유자의 프로젝트들을 실시간 관찰
     * @param ownerId 소유자 ID
     * @return 프로젝트 목록 Flow
     */
    fun observeProjectsByOwner(ownerId: String): Flow<List<Project>>

    /**
     * 특정 상태의 프로젝트들을 실시간 관찰
     * @param status 프로젝트 상태
     * @return 프로젝트 목록 Flow
     */
    fun observeProjectsByStatus(status: ProjectStatus): Flow<List<Project>>

    /**
     * 주어진 ID 목록에 해당하는 프로젝트 목록을 실시간 관찰
     * @param projectIds 프로젝트 ID 목록
     * @return 프로젝트 목록 Flow
     */
    fun observeProjects(projectIds: List<String>): Flow<List<Project>>

    /**
     * 특정 프로젝트의 updatedAt 필드 변경을 실시간 관찰
     * @param projectId 프로젝트 ID
     * @return updatedAt 타임스탬프 Flow
     */
    fun observeProjectUpdatedAt(projectId: String): Flow<Long?>

    /**
     * 모든 프로젝트를 실시간 관찰
     * @return 전체 프로젝트 목록 Flow
     */
    fun observeAllProjects(): Flow<List<Project>>

    /**
     * 활성 프로젝트들을 실시간 관찰
     * @return 활성 프로젝트 목록 Flow
     */
    fun observeActiveProjects(): Flow<List<Project>>

    /**
     * 사용자가 참여 중인 프로젝트들을 실시간 관찰
     * @param userId 사용자 ID
     * @return 참여 중인 프로젝트 목록 Flow
     */
    fun observeProjectsByMember(userId: String): Flow<List<Project>>

    // === 단순 읽기 작업 ===

    /**
     * 프로젝트 ID로 조회
     * @param projectId 프로젝트 ID
     * @return 프로젝트 (없으면 null)
     */
    suspend fun getProjectById(projectId: String): Project?

    /**
     * 프로젝트 이름으로 조회 (정확히 일치)
     * @param name 프로젝트 이름
     * @return 프로젝트 (없으면 null)
     */
    suspend fun getProjectByName(name: ProjectName): Project?

    /**
     * 프로젝트 이름으로 검색 (부분 일치)
     * @param name 검색할 이름
     * @param limit 제한 개수
     * @return 프로젝트 목록
     */
    suspend fun searchProjectsByName(name: String, limit: Int = 10): List<Project>

    /**
     * 여러 프로젝트 ID로 조회
     * @param projectIds 프로젝트 ID 목록
     * @return 프로젝트 목록
     */
    suspend fun getProjectsByIds(projectIds: List<String>): List<Project>

    /**
     * 전체 프로젝트 조회
     * @param limit 제한 개수 (null이면 전체)
     * @return 프로젝트 목록
     */
    suspend fun getAllProjects(limit: Int? = null): List<Project>

    /**
     * 특정 소유자의 프로젝트들 조회
     * @param ownerId 소유자 ID
     * @return 프로젝트 목록
     */
    suspend fun getProjectsByOwner(ownerId: String): List<Project>

    /**
     * 특정 상태의 프로젝트들 조회
     * @param status 프로젝트 상태
     * @return 프로젝트 목록
     */
    suspend fun getProjectsByStatus(status: ProjectStatus): List<Project>

    /**
     * 활성 프로젝트들 조회
     * @return 활성 프로젝트 목록
     */
    suspend fun getActiveProjects(): List<Project>

    /**
     * 사용자가 참여 중인 프로젝트들 조회
     * @param userId 사용자 ID
     * @return 참여 중인 프로젝트 목록
     */
    suspend fun getProjectsByMember(userId: String): List<Project>

    // === 쓰기 작업 (Outbox 포함) ===

    /**
     * 프로젝트 저장 (생성/수정)
     * @param project 저장할 프로젝트
     * @return 성공 여부
     */
    suspend fun saveProject(project: Project): CustomResult<Unit, Exception>

    /**
     * 프로젝트 대량 저장 (동기화용)
     * @param projects 저장할 프로젝트 목록
     * @return 성공 여부
     */
    suspend fun saveProjects(projects: List<Project>): CustomResult<Unit, Exception>

    /**
     * 프로젝트 삭제 (Soft Delete)
     * @param projectId 프로젝트 ID
     * @return 성공 여부
     */
    suspend fun deleteProject(projectId: String): CustomResult<Unit, Exception>

    /**
     * 프로젝트 정보 업데이트 (로컬)
     * @param projectId 프로젝트 ID
     * @param name 새로운 이름 (nullable)
     * @param status 새로운 상태 (nullable)
     * @return 성공 여부
     */
    suspend fun updateProject(
        projectId: String,
        name: ProjectName? = null,
        status: ProjectStatus? = null
    ): CustomResult<Unit, Exception>

    /**
     * 프로젝트 상태 변경
     * @param projectId 프로젝트 ID
     * @param newStatus 새로운 상태
     * @return 성공 여부
     */
    suspend fun changeProjectStatus(
        projectId: String,
        newStatus: ProjectStatus
    ): CustomResult<Unit, Exception>

    /**
     * 프로젝트 이름 변경
     * @param projectId 프로젝트 ID
     * @param newName 새로운 이름
     * @return 성공 여부
     */
    suspend fun changeProjectName(
        projectId: String,
        newName: ProjectName
    ): CustomResult<Unit, Exception>

    // === 유틸리티 ===

    /**
     * 프로젝트 존재 여부 확인
     * @param projectId 프로젝트 ID
     * @return 존재 여부
     */
    suspend fun projectExists(projectId: String): Boolean

    /**
     * 프로젝트 이름 중복 확인
     * @param name 프로젝트 이름
     * @param excludeProjectId 제외할 프로젝트 ID (수정시 자기 자신 제외)
     * @return 중복 여부
     */
    suspend fun nameExists(name: ProjectName, excludeProjectId: String? = null): Boolean

    /**
     * 전체 프로젝트 수 조회
     * @return 프로젝트 수
     */
    suspend fun getTotalProjectCount(): Int

    /**
     * 특정 소유자의 프로젝트 수 조회
     * @param ownerId 소유자 ID
     * @return 프로젝트 수
     */
    suspend fun getProjectCountByOwner(ownerId: String): Int

    /**
     * 특정 상태의 프로젝트 수 조회
     * @param status 프로젝트 상태
     * @return 프로젝트 수
     */
    suspend fun getProjectCountByStatus(status: ProjectStatus): Int

    /**
     * 활성 프로젝트 수 조회
     * @return 활성 프로젝트 수
     */
    suspend fun getActiveProjectCount(): Int

    /**
     * 모든 프로젝트 삭제 (초기화)
     * @return 성공 여부
     */
    suspend fun clearAllProjects(): CustomResult<Unit, Exception>

    // === 동기화 지원 ===

    /**
     * 특정 시간 이후 업데이트된 프로젝트 조회
     * @param timestamp 기준 시간
     * @return 업데이트된 프로젝트 목록
     */
    suspend fun getProjectsUpdatedAfter(timestamp: Instant): List<Project>

    /**
     * Outbox에 작업 추가 (서버 동기화 대기열)
     * @param projectId 프로젝트 ID
     * @param operation 작업 타입 (CREATE, UPDATE, DELETE)
     * @param payload 작업 데이터 (JSON)
     * @return 성공 여부
     */
    suspend fun addToOutbox(
        projectId: String,
        operation: String,
        payload: String? = null
    ): CustomResult<Unit, Exception>
}