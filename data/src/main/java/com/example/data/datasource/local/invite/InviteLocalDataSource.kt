package com.example.data.datasource.local.invite

import com.example.domain.model.Invite
import com.example.domain.model.ProjectInfo

/**
 * 초대 관련 로컬 데이터 소스 인터페이스
 * Room 데이터베이스를 사용한 로컬 캐싱 작업을 정의합니다.
 */
interface InviteLocalDataSource {
    /**
     * 초대 정보를 로컬에 저장합니다.
     * 
     * @param invite 초대 토큰 정보
     */
    suspend fun saveInvite(invite: Invite)
    
    /**
     * 모든 초대 정보를 일괄 저장합니다.
     * 
     * @param invites 초대 토큰 정보 목록
     */
    suspend fun saveInvites(invites: List<Invite>)
    
    /**
     * 초대 정보를 로컬에서 가져옵니다.
     * 
     * @param token 초대 토큰
     * @return 초대 정보 또는 null
     */
    suspend fun getInvite(token: String): Invite?
    
    /**
     * 프로젝트 관련 초대 정보를 모두 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 초대 정보 목록
     */
    suspend fun getInvitesByProject(projectId: String): List<Invite>
    
    /**
     * 초대 정보를 삭제합니다.
     * 
     * @param token 초대 토큰
     */
    suspend fun deleteInvite(token: String)
    
    /**
     * 프로젝트 관련 초대 정보를 모두 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     */
    suspend fun deleteInvitesByProject(projectId: String)
    
    /**
     * 만료된 초대 정보를 모두 삭제합니다.
     * 
     * @param currentTimeMillis 현재 시간 (밀리초)
     */
    suspend fun deleteExpiredInvites(currentTimeMillis: Long)
} 