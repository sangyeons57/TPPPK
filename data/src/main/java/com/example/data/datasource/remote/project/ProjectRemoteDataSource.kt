package com.example.data.datasource.remote.project

import com.example.data.model.remote.project.ProjectDto
import kotlinx.coroutines.flow.Flow

/**
 * Firestore 'projects' 컬렉션 또는 관련 프로젝트 데이터와 상호작용하는 데이터 소스 인터페이스입니다.
 */
interface ProjectRemoteDataSource {
    /**
     * 사용자가 참여하고 있는 프로젝트 목록을 가져옵니다.
     *
     * @param userId 사용자 ID.
     * @return kotlin.Result 객체. 성공 시 List<ProjectDto>, 실패 시 Exception 포함.
     */
    suspend fun getParticipatingProjects(userId: String): Result<List<ProjectDto>>

    /**
     * 프로젝트 상세 정보를 가져옵니다.
     *
     * @param projectId 프로젝트 ID.
     * @return kotlin.Result 객체. 성공 시 ProjectDto, 실패 시 Exception 포함.
     */
    suspend fun getProjectDetails(projectId: String): Result<ProjectDto>

    /**
     * 새 프로젝트를 생성합니다.
     *
     * @param projectDto 생성할 프로젝트 정보 DTO.
     * @return kotlin.Result 객체. 성공 시 생성된 프로젝트 ID(String), 실패 시 Exception 포함.
     */
    suspend fun createProject(projectDto: ProjectDto): Result<String>

    // ... 향후 필요한 프로젝트 관련 원격 데이터 처리 함수 추가 ...
} 