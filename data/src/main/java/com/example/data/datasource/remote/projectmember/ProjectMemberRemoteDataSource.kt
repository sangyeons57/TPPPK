package com.example.data.datasource.remote.projectmember

import com.example.domain.model.ProjectMember
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 멤버 관련 원격 데이터 소스 인터페이스
 * 프로젝트 멤버 관련 Firebase Firestore 작업을 정의합니다.
 */
interface ProjectMemberRemoteDataSource {
    /**
     * 특정 프로젝트의 모든 멤버 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 목록
     */
    suspend fun getProjectMembers(projectId: String): Result<List<ProjectMember>>
    
    /**
     * 특정 프로젝트의 멤버 목록 실시간 스트림을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 목록의 Flow
     */
    fun getProjectMembersStream(projectId: String): Flow<List<ProjectMember>>
    
    /**
     * 프로젝트에 새 멤버를 추가합니다.
     * @param projectId 프로젝트 ID
     * @param userId 추가할 사용자 ID
     * @param roleIds 부여할
     * @return 작업 성공 여부
     */
    suspend fun addMemberToProject(projectId: String, userId: String, roleIds: List<String>): Result<Unit>
    
    /**
     * 프로젝트에서 멤버를 제거합니다.
     * @param projectId 프로젝트 ID
     * @param userId 제거할 사용자 ID
     * @return 작업 성공 여부
     */
    suspend fun removeMemberFromProject(projectId: String, userId: String): Result<Unit>
    
    /**
     * 멤버의 역할을 업데이트합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param roleIds 새로운 역할 ID 목록
     * @return 작업 성공 여부
     */
    suspend fun updateMemberRoles(projectId: String, userId: String, roleIds: List<String>): Result<Unit>
} 