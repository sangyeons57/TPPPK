package com.example.data.db.dao

import androidx.room.*
import com.example.data.model.local.CategoryEntity
import com.example.data.model.local.ChannelEntity
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 구조(카테고리, 채널) 관련 데이터베이스 작업을 위한 DAO 인터페이스
 */
@Dao
interface ProjectStructureDao {
    // 카테고리 관련 메서드

    /**
     * 카테고리 정보를 삽입합니다. 이미 존재하는 경우 갱신합니다.
     * @param category 카테고리 엔티티
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    /**
     * 카테고리 목록을 삽입합니다. 이미 존재하는 경우 갱신합니다.
     * @param categories 카테고리 엔티티 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    /**
     * 특정 프로젝트의 모든 카테고리를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 카테고리 엔티티 목록
     */
    @Query("SELECT * FROM categories WHERE projectId = :projectId ORDER BY `order`")
    suspend fun getCategoriesByProjectId(projectId: String): List<CategoryEntity>

    /**
     * 특정 프로젝트의 모든 카테고리를 실시간으로 관찰합니다.
     * @param projectId 프로젝트 ID
     * @return 카테고리 엔티티 목록 Flow
     */
    @Query("SELECT * FROM categories WHERE projectId = :projectId ORDER BY `order`")
    fun observeCategoriesByProjectId(projectId: String): Flow<List<CategoryEntity>>

    /**
     * 카테고리 상세 정보를 가져옵니다.
     * @param categoryId 카테고리 ID
     * @return 카테고리 엔티티 또는 null
     */
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): CategoryEntity?

    /**
     * 카테고리를 삭제합니다.
     * @param categoryId 카테고리 ID
     */
    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)

    /**
     * 특정 프로젝트의 모든 카테고리를 삭제합니다.
     * @param projectId 프로젝트 ID
     */
    @Query("DELETE FROM categories WHERE projectId = :projectId")
    suspend fun deleteCategoriesByProjectId(projectId: String)

    // 채널 관련 메서드

    /**
     * 채널 정보를 삽입합니다. 이미 존재하는 경우 갱신합니다.
     * @param channel 채널 엔티티
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    /**
     * 채널 목록을 삽입합니다. 이미 존재하는 경우 갱신합니다.
     * @param channels 채널 엔티티 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    /**
     * 특정 프로젝트의 모든 채널을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 채널 엔티티 목록
     */
    @Query("SELECT * FROM channels WHERE projectId = :projectId ORDER BY `channelOrder`")
    suspend fun getChannelsByProjectId(projectId: String): List<ChannelEntity>

    /**
     * 특정 카테고리의 모든 채널을 가져옵니다.
     * @param categoryId 카테고리 ID
     * @return 채널 엔티티 목록
     */
    @Query("SELECT * FROM channels WHERE categoryId = :categoryId ORDER BY `channelOrder`")
    suspend fun getChannelsByCategoryId(categoryId: String): List<ChannelEntity>

    /**
     * 특정 프로젝트의 모든 채널을 실시간으로 관찰합니다.
     * @param projectId 프로젝트 ID
     * @return 채널 엔티티 목록 Flow
     */
    @Query("SELECT * FROM channels WHERE projectId = :projectId ORDER BY `channelOrder`")
    fun observeChannelsByProjectId(projectId: String): Flow<List<ChannelEntity>>

    /**
     * 특정 카테고리의 모든 채널을 실시간으로 관찰합니다.
     * @param categoryId 카테고리 ID
     * @return 채널 엔티티 목록 Flow
     */
    @Query("SELECT * FROM channels WHERE categoryId = :categoryId ORDER BY `channelOrder`")
    fun observeChannelsByCategoryId(categoryId: String): Flow<List<ChannelEntity>>

    /**
     * 채널 상세 정보를 가져옵니다.
     * @param channelId 채널 ID
     * @return 채널 엔티티 또는 null
     */
    @Query("SELECT * FROM channels WHERE id = :channelId")
    suspend fun getChannelById(channelId: String): ChannelEntity?

    /**
     * 채널을 삭제합니다.
     * @param channelId 채널 ID
     */
    @Query("DELETE FROM channels WHERE id = :channelId")
    suspend fun deleteChannel(channelId: String)

    /**
     * 특정 카테고리의 모든 채널을 삭제합니다.
     * @param categoryId 카테고리 ID
     */
    @Query("DELETE FROM channels WHERE categoryId = :categoryId")
    suspend fun deleteChannelsByCategoryId(categoryId: String)

    /**
     * 특정 프로젝트의 모든 채널을 삭제합니다.
     * @param projectId 프로젝트 ID
     */
    @Query("DELETE FROM channels WHERE projectId = :projectId")
    suspend fun deleteChannelsByProjectId(projectId: String)
} 