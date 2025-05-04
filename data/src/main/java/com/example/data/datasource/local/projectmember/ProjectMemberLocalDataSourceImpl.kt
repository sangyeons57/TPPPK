package com.example.data.datasource.local.projectmember

import com.example.data.db.dao.ProjectMemberDao
import com.example.data.model.local.ProjectMemberEntity
import com.example.domain.model.ProjectMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProjectMemberLocalDataSource 인터페이스의 구현체입니다.
 * Room 데이터베이스를 사용하여 프로젝트 멤버 관련 로컬 CRUD 작업을 수행합니다.
 * 
 * @param projectMemberDao 프로젝트 멤버 관련 데이터베이스 액세스 객체
 */
@Singleton
class ProjectMemberLocalDataSourceImpl @Inject constructor(
    private val projectMemberDao: ProjectMemberDao
) : ProjectMemberLocalDataSource {

    /**
     * 특정 프로젝트의 모든 멤버 목록을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 목록
     */
    override suspend fun getProjectMembers(projectId: String): List<ProjectMember> {
        return projectMemberDao.getProjectMembers(projectId).map { it.toDomain() }
    }

    /**
     * 특정 프로젝트의 멤버 목록 실시간 스트림을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 멤버 목록의 Flow
     */
    override fun getProjectMembersStream(projectId: String): Flow<List<ProjectMember>> {
        return projectMemberDao.observeProjectMembers(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * 특정 프로젝트 멤버 정보를 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return 프로젝트 멤버 또는 null
     */
    override suspend fun getProjectMember(projectId: String, userId: String): ProjectMember? {
        return projectMemberDao.getProjectMember(projectId, userId)?.toDomain()
    }

    /**
     * 특정 프로젝트의 멤버 목록을 저장합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param members 프로젝트 멤버 목록
     */
    override suspend fun saveProjectMembers(projectId: String, members: List<ProjectMember>) {
        val entities = members.map { ProjectMemberEntity.fromDomain(projectId, it) }
        projectMemberDao.insertProjectMembers(entities)
    }

    /**
     * 특정 프로젝트 멤버 정보를 저장합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param member 프로젝트 멤버
     */
    override suspend fun saveProjectMember(projectId: String, member: ProjectMember) {
        val entity = ProjectMemberEntity.fromDomain(projectId, member)
        projectMemberDao.insertProjectMember(entity)
    }

    /**
     * 특정 프로젝트 멤버를 제거합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return 제거 성공 여부
     */
    override suspend fun removeProjectMember(projectId: String, userId: String): Boolean {
        return projectMemberDao.deleteProjectMember(projectId, userId) > 0
    }

    /**
     * 특정 프로젝트의 모든 멤버를 제거합니다.
     * 
     * @param projectId 프로젝트 ID
     */
    override suspend fun clearProjectMembers(projectId: String) {
        projectMemberDao.deleteProjectMembers(projectId)
    }
} 