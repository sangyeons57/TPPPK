package com.example.data.db.dao

import androidx.room.*
import com.example.data.model.local.ProjectMemberEntity
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 멤버 관련 데이터베이스 액세스 인터페이스
 */
@Dao
interface ProjectMemberDao {
    /**
     * 특정 프로젝트의 모든 멤버를 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 목록
     */
    @Query("SELECT * FROM project_members WHERE projectId = :projectId ORDER BY userName")
    suspend fun getProjectMembers(projectId: String): List<ProjectMemberEntity>
    
    /**
     * 특정 프로젝트의 모든 멤버를 Flow로 관찰합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 목록 Flow
     */
    @Query("SELECT * FROM project_members WHERE projectId = :projectId ORDER BY userName")
    fun observeProjectMembers(projectId: String): Flow<List<ProjectMemberEntity>>
    
    /**
     * 특정 프로젝트와 사용자 ID로 멤버 정보를 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return 프로젝트 멤버 엔티티 또는 null
     */
    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND userId = :userId LIMIT 1")
    suspend fun getProjectMember(projectId: String, userId: String): ProjectMemberEntity?
    
    /**
     * 새 프로젝트 멤버를 삽입합니다.
     * 
     * @param member 삽입할 프로젝트 멤버 엔티티
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectMember(member: ProjectMemberEntity)
    
    /**
     * 여러 프로젝트 멤버를 삽입합니다.
     * 
     * @param members 삽입할 프로젝트 멤버 엔티티 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectMembers(members: List<ProjectMemberEntity>)
    
    /**
     * 프로젝트 멤버 정보를 업데이트합니다.
     * 
     * @param member 업데이트할 프로젝트 멤버 엔티티
     * @return 영향받은 행 수
     */
    @Update
    suspend fun updateProjectMember(member: ProjectMemberEntity): Int
    
    /**
     * 프로젝트 멤버를 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return 영향받은 행 수
     */
    @Query("DELETE FROM project_members WHERE projectId = :projectId AND userId = :userId")
    suspend fun deleteProjectMember(projectId: String, userId: String): Int
    
    /**
     * 특정 프로젝트의 모든 멤버를 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 영향받은 행 수
     */
    @Query("DELETE FROM project_members WHERE projectId = :projectId")
    suspend fun deleteProjectMembers(projectId: String): Int
    
    /**
     * 특정 사용자의 모든 프로젝트 멤버십을 삭제합니다.
     * 
     * @param userId 사용자 ID
     * @return 영향받은 행 수
     */
    @Query("DELETE FROM project_members WHERE userId = :userId")
    suspend fun deleteUserMemberships(userId: String): Int
} 