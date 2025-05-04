package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.data.model.local.ProjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room 데이터베이스의 'projects' 테이블에 접근하기 위한 DAO(Data Access Object) 인터페이스입니다.
 */
@Dao
interface ProjectDao {

    /**
     * 모든 프로젝트 목록을 Flow 형태로 가져옵니다.
     * @return 프로젝트 엔티티 리스트의 Flow.
     */
    @Query("SELECT * FROM projects")
    fun getAllProjectsStream(): Flow<List<ProjectEntity>>

    /**
     * 특정 ID를 가진 프로젝트 정보를 가져옵니다.
     * @param projectId 가져올 프로젝트의 ID.
     * @return 해당 ID의 프로젝트 엔티티. 없으면 null.
     */
    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    /**
     * 특정 ID를 가진 프로젝트 정보를 Flow 형태로 가져옵니다.
     * @param projectId 가져올 프로젝트의 ID.
     * @return 해당 ID의 프로젝트 엔티티 Flow. 없으면 null 방출.
     */
    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    fun getProjectStream(projectId: String): Flow<ProjectEntity?>

    /**
     * 사용자가 참여하고 있는 모든 프로젝트 목록을 Flow 형태로 가져옵니다.
     * TypeConverter를 통해 participantIds가 JSON 문자열로 저장되어 있다고 가정하고 LIKE 연산자를 사용합니다.
     * @param userId 사용자 ID.
     * @return ProjectEntity 리스트의 Flow.
     */
    @Query("SELECT * FROM projects WHERE participantIds LIKE '%' || :userId || '%' ORDER BY createdAt DESC")
    fun getParticipatingProjectsStream(userId: String): Flow<List<ProjectEntity>>

    /**
     * 새로운 프로젝트 목록을 삽입합니다. 기존 데이터는 삭제됩니다.
     * @param projects 저장할 프로젝트 엔티티 리스트.
     */
    @Transaction
    suspend fun clearAndInsertProjects(projects: List<ProjectEntity>) {
        clearAllProjects()
        insertProjects(projects)
    }

    /**
     * 여러 프로젝트를 한 번에 삽입합니다. 충돌 시 무시합니다.
     * @param projects 삽입할 프로젝트 엔티티 리스트.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    /**
     * 단일 프로젝트를 삽입하거나 이미 존재하면 업데이트합니다 (Upsert).
     * @param project 추가 또는 업데이트할 프로젝트 엔티티.
     */
    @Upsert
    suspend fun upsertProject(project: ProjectEntity)

    /**
     * 프로젝트 정보를 업데이트합니다.
     * @param project 업데이트할 프로젝트 엔티티.
     * @return 업데이트된 행의 수.
     */
    @Update
    suspend fun updateProject(project: ProjectEntity): Int

    /**
     * 특정 ID의 프로젝트를 삭제합니다.
     * @param projectId 삭제할 프로젝트의 ID.
     * @return 삭제된 행의 수.
     */
    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String): Int

    /**
     * 프로젝트를 삭제합니다.
     * @param project 삭제할 프로젝트 엔티티.
     * @return 삭제된 행의 수.
     */
    @Delete
    suspend fun deleteProject(project: ProjectEntity): Int

    /**
     * 모든 프로젝트를 삭제합니다.
     */
    @Query("DELETE FROM projects")
    suspend fun clearAllProjects()
} 