package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.local.InviteEntity

/**
 * 초대 관련 데이터 액세스 객체 인터페이스
 * Room 데이터베이스에서 초대 관련 쿼리를 정의합니다.
 */
@Dao
interface InviteDao {
    /**
     * 초대 정보를 저장합니다. 이미 존재하면 대체합니다.
     * 
     * @param invite 저장할 초대 정보
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvite(invite: InviteEntity)
    
    /**
     * 여러 초대 정보를 일괄 저장합니다. 이미 존재하면 대체합니다.
     * 
     * @param invites 저장할 초대 정보 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvites(invites: List<InviteEntity>)
    
    /**
     * 특정 토큰의 초대 정보를 가져옵니다.
     * 
     * @param token 초대 토큰
     * @return 초대 정보 또는 null
     */
    @Query("SELECT * FROM invites WHERE token = :token")
    suspend fun getInvite(token: String): InviteEntity?
    
    /**
     * 특정 프로젝트 관련 초대 정보를 모두 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 초대 정보 목록
     */
    @Query("SELECT * FROM invites WHERE projectId = :projectId")
    suspend fun getInvitesByProject(projectId: String): List<InviteEntity>
    
    /**
     * 특정 토큰의 초대 정보를 삭제합니다.
     * 
     * @param token 초대 토큰
     * @return 삭제된 항목 수
     */
    @Query("DELETE FROM invites WHERE token = :token")
    suspend fun deleteInvite(token: String): Int
    
    /**
     * 특정 프로젝트 관련 초대 정보를 모두 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 삭제된 항목 수
     */
    @Query("DELETE FROM invites WHERE projectId = :projectId")
    suspend fun deleteInvitesByProject(projectId: String): Int
    
    /**
     * 만료된 초대 정보를 모두 삭제합니다.
     * 
     * @param currentTimeMillis 현재 시간 (밀리초)
     * @return 삭제된 항목 수
     */
    @Query("DELETE FROM invites WHERE expiresAt < :currentTimeMillis")
    suspend fun deleteExpiredInvites(currentTimeMillis: Long): Int
} 