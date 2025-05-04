package com.example.data.datasource.local.projectmember

import com.example.domain.model.ProjectMember
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 멤버 관련 로컬 데이터 소스 인터페이스
 * Room 데이터베이스를 사용하여 프로젝트 멤버 관련 로컬 CRUD 작업을 정의합니다.
 */
interface ProjectMemberLocalDataSource {
    /**
     * 특정 프로젝트의 모든 멤버 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 목록
     */
    suspend fun getProjectMembers(projectId: String): List<ProjectMember>
    
    /**
     * 특정 프로젝트의 멤버 목록 실시간 스트림을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 목록의 Flow
     */
    fun getProjectMembersStream(projectId: String): Flow<List<ProjectMember>>
    
    /**
     * 특정 프로젝트 멤버 정보를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return 프로젝트 멤버 또는 null
     */
    suspend fun getProjectMember(projectId: String, userId: String): ProjectMember?
    
    /**
     * 특정 프로젝트의 멤버 목록을 저장합니다.
     * @param projectId 프로젝트 ID
     * @param members 프로젝트 멤버 목록
     */
    suspend fun saveProjectMembers(projectId: String, members: List<ProjectMember>)
    
    /**
     * 특정 프로젝트 멤버 정보를 저장합니다.
     * @param projectId 프로젝트 ID
     * @param member 프로젝트 멤버
     */
    suspend fun saveProjectMember(projectId: String, member: ProjectMember)
    
    /**
     * 특정 프로젝트 멤버를 제거합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return 제거 성공 여부
     */
    suspend fun removeProjectMember(projectId: String, userId: String): Boolean
    
    /**
     * 특정 프로젝트의 모든 멤버를 제거합니다.
     * @param projectId 프로젝트 ID
     */
    suspend fun clearProjectMembers(projectId: String)
} 