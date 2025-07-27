package com.example.data.datasource.local

import com.example.domain.model.base.Project
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 로컬 프로젝트 데이터 저장소 인터페이스
 * 3-tier 클라이언트 주도 동기화 아키텍처를 지원합니다
 */
interface LocalProjectsDataSource {

    // === 기본 CRUD 작업 ===

    /**
     * 프로젝트 ID로 단일 프로젝트 조회
     * @param projectId 프로젝트 ID
     * @return 프로젝트 정보 (없으면 null)
     */
    suspend fun getProjectById(projectId: String): Project?

    /**
     * 모든 프로젝트 목록 조회
     * @return 프로젝트 목록
     */
    suspend fun getAllProjects(): List<Project>

    /**
     * 소유자 ID로 프로젝트 목록 조회
     * @param ownerId 소유자 ID
     * @return 프로젝트 목록
     */
    suspend fun getProjectsByOwner(ownerId: String): List<Project>

    /**
     * 프로젝트 상태별 프로젝트 목록 조회
     * @param status 프로젝트 상태 (ACTIVE, ARCHIVED, DELETED)
     * @return 프로젝트 목록
     */
    suspend fun getProjectsByStatus(status: String): List<Project>

    /**
     * 프로젝트 이름으로 검색
     * @param nameQuery 검색할 이름 (부분 일치)
     * @return 프로젝트 목록
     */
    suspend fun searchProjectsByName(nameQuery: String): List<Project>

    /**
     * 단일 프로젝트 정보 저장
     * @param project 저장할 프로젝트 정보
     */
    suspend fun saveProject(project: Project)

    /**
     * 프로젝트 목록 배치 저장
     * @param projects 저장할 프로젝트 목록
     */
    suspend fun saveProjects(projects: List<Project>)

    /**
     * 프로젝트 삭제
     * @param projectId 삭제할 프로젝트 ID
     */
    suspend fun deleteProject(projectId: String)

    // === 3-tier 동기화 지원 ===

    /**
     * 특정 시점 이후 업데이트된 프로젝트들을 조회 (증분 동기화용)
     * @param timestamp 기준 시간
     * @return 업데이트된 프로젝트 목록
     */
    suspend fun getProjectsUpdatedAfter(timestamp: Instant): List<Project>

    /**
     * 프로젝트 실시간 관찰
     * @param projectId 프로젝트 ID
     * @return 프로젝트 정보 Flow
     */
    fun observeProjectById(projectId: String): Flow<Project?>

    /**
     * 모든 프로젝트 실시간 관찰
     * @return 프로젝트 목록 Flow
     */
    fun observeAllProjects(): Flow<List<Project>>

    /**
     * 소유자별 프로젝트 실시간 관찰
     * @param ownerId 소유자 ID
     * @return 프로젝트 목록 Flow
     */
    fun observeProjectsByOwner(ownerId: String): Flow<List<Project>>

    // === Outbox 관리 ===

    /**
     * 프로젝트 변경사항을 Outbox에 기록
     * @param projectId 프로젝트 ID
     * @param operation 작업 유형 (CREATE, UPDATE, DELETE)
     * @param payload 변경 데이터 (선택적)
     */
    suspend fun addToOutbox(projectId: String, operation: String, payload: String? = null)

    /**
     * 대기 중인 Outbox 작업 목록 조회
     * @return 대기 중인 작업 목록
     */
    suspend fun getPendingOutboxOperations(): List<ProjectOutboxOperation>

    /**
     * Outbox 작업 완료 처리
     * @param operationId 작업 ID
     */
    suspend fun markOutboxOperationComplete(operationId: String)

    /**
     * Outbox 작업 재시도 증가
     * @param operationId 작업 ID
     */
    suspend fun incrementOutboxRetries(operationId: String)

    // === 동기화 메타데이터 관리 ===

    /**
     * 마지막 동기화 커서 조회
     * @return 마지막 서버 커서 (밀리초)
     */
    suspend fun getLastSyncCursor(): Long?

    /**
     * 동기화 커서 업데이트
     * @param cursor 새로운 서버 커서
     * @param timestamp 동기화 시간
     */
    suspend fun updateSyncCursor(cursor: Long, timestamp: Long)

    // === 유틸리티 ===

    /**
     * 프로젝트 존재 여부 확인
     * @param projectId 프로젝트 ID
     * @return 존재 여부
     */
    suspend fun projectExists(projectId: String): Boolean

    /**
     * 전체 프로젝트 수 조회
     * @return 프로젝트 수
     */
    suspend fun getProjectCount(): Int

    /**
     * 소유자별 프로젝트 수 조회
     * @param ownerId 소유자 ID
     * @return 프로젝트 수
     */
    suspend fun getProjectCountByOwner(ownerId: String): Int

    /**
     * 모든 프로젝트 데이터 삭제 (개발/테스트용)
     */
    suspend fun clearAllProjects()
}

/**
 * 프로젝트 Outbox 작업 정보
 */
data class ProjectOutboxOperation(
    val id: String,
    val projectId: String,
    val operation: String,
    val payload: String?,
    val localTimestamp: Long,
    val retries: Int
)