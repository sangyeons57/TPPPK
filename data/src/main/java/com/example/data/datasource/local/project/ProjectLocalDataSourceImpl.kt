package com.example.data.datasource.local.project

import com.example.data.db.dao.ProjectDao // Room DAO 위치 가정
import com.example.data.model.local.ProjectEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProjectLocalDataSource 인터페이스의 Room 데이터베이스 구현체입니다.
 */
@Singleton
class ProjectLocalDataSourceImpl @Inject constructor(
    private val projectDao: ProjectDao // Room DAO 주입
) : ProjectLocalDataSource {

    // --- ProjectLocalDataSource 인터페이스 함수 구현 --- 

    /*
    override fun getAllProjectsStream(): Flow<List<ProjectEntity>> {
        // Room DAO를 사용하여 모든 프로젝트 스트림 조회 로직 구현
        // 예: return projectDao.getAllProjectsStream()
        throw NotImplementedError("getAllProjectsStream not implemented yet")
    }
    */

    /*
    override suspend fun getProjectById(projectId: String): ProjectEntity? {
        // Room DAO를 사용하여 특정 프로젝트 조회 로직 구현
        // 예: return projectDao.getProjectById(projectId)
        throw NotImplementedError("getProjectById not implemented yet")
    }
    */

    /*
    override suspend fun saveProjects(projects: List<ProjectEntity>) {
        // Room DAO를 사용하여 프로젝트 목록 저장 로직 구현
        // 예: projectDao.clearAndInsertProjects(projects)
        throw NotImplementedError("saveProjects not implemented yet")
    }
    */

    /**
     * 특정 프로젝트 정보를 Flow 형태로 가져옵니다.
     * @param projectId 가져올 프로젝트의 ID.
     * @return ProjectEntity의 Flow. 프로젝트가 없으면 null을 방출하는 Flow.
     */
    override fun getProjectStream(projectId: String): Flow<ProjectEntity?> {
        return projectDao.getProjectStream(projectId)
    }

    /**
     * 사용자가 참여하고 있는 모든 프로젝트 목록을 Flow 형태로 가져옵니다.
     * @param userId 사용자 ID.
     * @return ProjectEntity 리스트의 Flow.
     */
    override fun getParticipatingProjectsStream(userId: String): Flow<List<ProjectEntity>> {
        // ProjectDao에 해당 쿼리 메서드가 정의되어 있다고 가정합니다.
        // 만약 없다면 ProjectDao에 추가해야 합니다.
        return projectDao.getParticipatingProjectsStream(userId)
    }

    /**
     * 프로젝트 정보를 삽입하거나 업데이트합니다 (Upsert).
     * @param project 추가 또는 업데이트할 프로젝트 엔티티.
     */
    override suspend fun upsertProject(project: ProjectEntity) {
        projectDao.upsertProject(project)
    }

    /**
     * 특정 프로젝트를 삭제합니다.
     * @param projectId 삭제할 프로젝트의 ID.
     */
    override suspend fun deleteProject(projectId: String) {
        projectDao.deleteProjectById(projectId)
    }

    // ... 다른 함수들의 실제 구현 추가 ...
} 