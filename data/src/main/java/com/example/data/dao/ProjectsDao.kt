package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.local.ProjectsEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * DAO for Projects collection
 * Provides CRUD operations for project data
 * Supports SSOT (Single Source of Truth) pattern
 */
@Dao
interface ProjectsDao {

    // === Basic CRUD Operations ===

    /**
     * Get project by ID
     */
    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: String): ProjectsEntity?

    /**
     * Get all projects
     */
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    suspend fun getAllProjects(): List<ProjectsEntity>

    /**
     * Get projects by owner
     */
    @Query("SELECT * FROM projects WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    suspend fun getProjectsByOwner(ownerId: String): List<ProjectsEntity>

    /**
     * Insert or update project
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectsEntity)

    /**
     * Insert or update multiple projects
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectsEntity>)

    /**
     * Update project
     */
    @Update
    suspend fun updateProject(project: ProjectsEntity)

    /**
     * Delete project by ID
     */
    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: String)

    // === Flow-based Real-time Observations ===

    /**
     * Observe project by ID
     */
    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeProjectById(projectId: String): Flow<ProjectsEntity?>

    /**
     * Observe all projects
     */
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeAllProjects(): Flow<List<ProjectsEntity>>

    /**
     * Observe projects by owner
     */
    @Query("SELECT * FROM projects WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun observeProjectsByOwner(ownerId: String): Flow<List<ProjectsEntity>>

    /**
     * Observe projects by status
     */
    @Query("SELECT * FROM projects WHERE status = :status ORDER BY createdAt DESC")
    fun observeProjectsByStatus(status: String): Flow<List<ProjectsEntity>>

    // === Incremental Sync Queries ===

    /**
     * Get projects updated after specific timestamp (for incremental sync)
     * 🔑 Core sync method: fetches projects changed since last sync
     */
    @Query("SELECT * FROM projects WHERE updatedAt > :timestamp ORDER BY updatedAt ASC")
    suspend fun getProjectsUpdatedAfter(timestamp: Instant): List<ProjectsEntity>

    /**
     * Get latest update timestamp for sync tracking
     */
    @Query("SELECT MAX(updatedAt) FROM projects")
    suspend fun getLatestUpdateTimestamp(): Instant?


    // === Search and Filter Queries ===

    /**
     * Search projects by name
     */
    @Query("SELECT * FROM projects WHERE name LIKE '%' || :searchTerm || '%' ORDER BY name ASC")
    suspend fun searchProjectsByName(searchTerm: String): List<ProjectsEntity>

    /**
     * Get projects by status
     */
    @Query("SELECT * FROM projects WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getProjectsByStatus(status: String): List<ProjectsEntity>

    /**
     * Get active projects
     */
    @Query("SELECT * FROM projects WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    suspend fun getActiveProjects(): List<ProjectsEntity>

    /**
     * Get archived projects
     */
    @Query("SELECT * FROM projects WHERE status = 'ARCHIVED' ORDER BY createdAt DESC")
    suspend fun getArchivedProjects(): List<ProjectsEntity>

    // === Utility Queries ===

    /**
     * Check if project exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM projects WHERE id = :projectId)")
    suspend fun projectExists(projectId: String): Boolean

    /**
     * Get total project count
     */
    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int

    /**
     * Get project count by status
     */
    @Query("SELECT COUNT(*) FROM projects WHERE status = :status")
    suspend fun getProjectCountByStatus(status: String): Int

    /**
     * Get project count by owner
     */
    @Query("SELECT COUNT(*) FROM projects WHERE ownerId = :ownerId")
    suspend fun getProjectCountByOwner(ownerId: String): Int

    // === Cleanup Operations ===

    /**
     * Delete all projects (for cache reset)
     */
    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()

    /**
     * Delete projects with specific status
     */
    @Query("DELETE FROM projects WHERE status = :status")
    suspend fun deleteProjectsByStatus(status: String)

    /**
     * Delete projects by owner
     */
    @Query("DELETE FROM projects WHERE ownerId = :ownerId")
    suspend fun deleteProjectsByOwner(ownerId: String)
}