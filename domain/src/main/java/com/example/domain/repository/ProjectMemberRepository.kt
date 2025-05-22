package com.example.domain.repository

import com.example.domain.model.ProjectMember
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 프로젝트 멤버 관련 저장소 인터페이스
 * 로컬 및 원격 데이터 소스를 조율하여 프로젝트 멤버 데이터를 관리합니다.
 */
interface ProjectMemberRepository {
    /**
     * 특정 프로젝트의 모든 멤버 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param forceRefresh 강제로 원격 데이터를 가져올지 여부
     * @return 프로젝트 멤버 목록 결과
     */
    suspend fun getProjectMembers(projectId: String, forceRefresh: Boolean = false): Result<List<ProjectMember>>
    
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
     * @param forceRefresh 강제로 원격 데이터를 가져올지 여부
     * @return 프로젝트 멤버 또는 null
     */
    suspend fun getProjectMember(projectId: String, userId: String, forceRefresh: Boolean = false): Result<ProjectMember?>
    
    /**
     * 프로젝트에 새 멤버를 추가합니다.
     * @param projectId 프로젝트 ID
     * @param userId 추가할 사용자 ID
     * @param roleIds 부여할 역할 ID 목록
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
    
    /**
     * 특정 프로젝트의 모든 멤버 데이터를 동기화합니다.
     * @param projectId 프로젝트 ID
     * @return 작업 성공 여부
     */
    suspend fun syncProjectMembers(projectId: String): Result<Unit>
    
    /**
     * 멤버 목록 새로고침 (호환성 유지용)
     * @param projectId 프로젝트 ID
     * @return 작업 성공 여부
     */
    suspend fun fetchProjectMembers(projectId: String): Result<Unit> = syncProjectMembers(projectId)
    
    /**
     * 멤버에게 채널 접근 권한을 추가합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param channelId 채널 ID
     * @return 작업 성공 여부
     */
    suspend fun addChannelAccessToMember(projectId: String, userId: String, channelId: String): Result<Unit>
    
    /**
     * 멤버의 채널 접근 권한을 제거합니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @param channelId 채널 ID
     * @return 작업 성공 여부
     */
    suspend fun removeChannelAccessFromMember(projectId: String, userId: String, channelId: String): Result<Unit>
    
    /**
     * 채널에 접근 가능한 모든 멤버 ID 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 멤버 ID 목록
     */
    suspend fun getMembersWithChannelAccess(projectId: String, channelId: String): Result<List<String>>
    
    /**
     * 멤버가 접근 가능한 모든 채널 ID 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return 채널 ID 목록
     */
    suspend fun getMemberChannelAccess(projectId: String, userId: String): Result<List<String>>
    
    /**
     * 사용자가 특정 채널에 접근할 수 있는지 확인합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param userId 사용자 ID
     * @return 접근 가능 여부
     */
    suspend fun canAccessChannel(projectId: String, channelId: String, userId: String): Result<Boolean>
} 