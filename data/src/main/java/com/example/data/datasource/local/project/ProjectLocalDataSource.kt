package com.example.data.datasource.local.project

import com.example.data.model.local.ProjectEntity // Local DB Entity 위치 가정
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 데이터의 로컬 데이터 소스 인터페이스입니다.
 * Room 데이터베이스와 상호작용합니다.
 */
interface ProjectLocalDataSource {
    // 예시: 모든 프로젝트 목록 스트림 가져오기
    // fun getAllProjectsStream(): Flow<List<ProjectEntity>>

    // 예시: 특정 프로젝트 정보 가져오기
    // suspend fun getProjectById(projectId: String): ProjectEntity?

    // 예시: 프로젝트 목록 저장 (기존 데이터 대체)
    // suspend fun saveProjects(projects: List<ProjectEntity>)

    /**
     * 특정 프로젝트 정보를 Flow 형태로 가져옵니다.
     * @param projectId 가져올 프로젝트의 ID.
     * @return ProjectEntity의 Flow. 프로젝트가 없으면 null을 방출하는 Flow.
     */
    fun getProjectStream(projectId: String): Flow<ProjectEntity?>

    /**
     * 사용자가 참여하고 있는 모든 프로젝트 목록을 Flow 형태로 가져옵니다.
     * @param userId 사용자 ID.
     * @return ProjectEntity 리스트의 Flow.
     */
    fun getParticipatingProjectsStream(userId: String): Flow<List<ProjectEntity>>

    /**
     * 프로젝트 정보를 삽입하거나 업데이트합니다 (Upsert).
     * @param project 추가 또는 업데이트할 프로젝트 엔티티.
     */
    suspend fun upsertProject(project: ProjectEntity)

    /**
     * 특정 프로젝트를 삭제합니다.
     * @param projectId 삭제할 프로젝트의 ID.
     */
    suspend fun deleteProject(projectId: String)

    // ... 필요한 프로젝트 관련 로컬 데이터 처리 함수 추가 ...
} 