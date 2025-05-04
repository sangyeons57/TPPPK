package com.example.data.datasource.local.invite

import com.example.data.db.dao.InviteDao
import com.example.data.model.local.InviteEntity
import com.example.domain.model.Invite
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 초대 관련 로컬 데이터 소스 구현체
 * Room 데이터베이스를 사용하여 초대 관련 로컬 캐싱 기능을 구현합니다.
 * 
 * @param inviteDao InviteDao 인스턴스
 */
@Singleton
class InviteLocalDataSourceImpl @Inject constructor(
    private val inviteDao: InviteDao
) : InviteLocalDataSource {

    /**
     * 초대 정보를 로컬에 저장합니다.
     * 
     * @param invite 초대 토큰 정보
     */
    override suspend fun saveInvite(invite: Invite) {
        val inviteEntity = InviteEntity.fromDomain(invite)
        inviteDao.insertInvite(inviteEntity)
    }
    
    /**
     * 모든 초대 정보를 일괄 저장합니다.
     * 
     * @param invites 초대 토큰 정보 목록
     */
    override suspend fun saveInvites(invites: List<Invite>) {
        val inviteEntities = invites.map { InviteEntity.fromDomain(it) }
        inviteDao.insertInvites(inviteEntities)
    }
    
    /**
     * 초대 정보를 로컬에서 가져옵니다.
     * 
     * @param token 초대 토큰
     * @return 초대 정보 또는 null
     */
    override suspend fun getInvite(token: String): Invite? {
        return inviteDao.getInvite(token)?.toDomain()
    }
    
    /**
     * 프로젝트 관련 초대 정보를 모두 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 초대 정보 목록
     */
    override suspend fun getInvitesByProject(projectId: String): List<Invite> {
        return inviteDao.getInvitesByProject(projectId).map { it.toDomain() }
    }
    
    /**
     * 초대 정보를 삭제합니다.
     * 
     * @param token 초대 토큰
     */
    override suspend fun deleteInvite(token: String) {
        inviteDao.deleteInvite(token)
    }
    
    /**
     * 프로젝트 관련 초대 정보를 모두 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     */
    override suspend fun deleteInvitesByProject(projectId: String) {
        inviteDao.deleteInvitesByProject(projectId)
    }
    
    /**
     * 만료된 초대 정보를 모두 삭제합니다.
     * 
     * @param currentTimeMillis 현재 시간 (밀리초)
     */
    override suspend fun deleteExpiredInvites(currentTimeMillis: Long) {
        inviteDao.deleteExpiredInvites(currentTimeMillis)
    }
} 