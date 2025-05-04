package com.example.data.datasource.local.projectstructure

import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ProjectStructure
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 구조(카테고리, 채널) 관련 로컬 데이터 소스 인터페이스
 * Room 데이터베이스를 사용하여 프로젝트 구조 관련 로컬 CRUD 작업을 정의합니다.
 */
interface ProjectStructureLocalDataSource {
    /**
     * 프로젝트 구조를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조
     */
    suspend fun getProjectStructure(projectId: String): ProjectStructure
    
    /**
     * 프로젝트 구조 실시간 스트림을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 Flow
     */
    fun getProjectStructureStream(projectId: String): Flow<ProjectStructure>
    
    /**
     * 특정 프로젝트의 모든 카테고리를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 카테고리 목록
     */
    suspend fun getCategoriesByProjectId(projectId: String): List<Category>
    
    /**
     * 특정 프로젝트의 모든 채널을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 채널 목록
     */
    suspend fun getChannelsByProjectId(projectId: String): List<Channel>
    
    /**
     * 특정 카테고리의 모든 채널을 가져옵니다.
     * @param categoryId 카테고리 ID
     * @return 채널 목록
     */
    suspend fun getChannelsByCategoryId(categoryId: String): List<Channel>
    
    /**
     * 카테고리 상세 정보를 가져옵니다.
     * @param categoryId 카테고리 ID
     * @return 카테고리 또는 null
     */
    suspend fun getCategoryById(categoryId: String): Category?
    
    /**
     * 채널 상세 정보를 가져옵니다.
     * @param channelId 채널 ID
     * @return 채널 또는 null
     */
    suspend fun getChannelById(channelId: String): Channel?
    
    /**
     * 카테고리를 저장합니다.
     * @param category 카테고리
     */
    suspend fun saveCategory(category: Category)
    
    /**
     * 카테고리 목록을 저장합니다.
     * @param categories 카테고리 목록
     */
    suspend fun saveCategories(categories: List<Category>)
    
    /**
     * 채널을 저장합니다.
     * @param channel 채널
     */
    suspend fun saveChannel(channel: Channel)
    
    /**
     * 채널 목록을 저장합니다.
     * @param channels 채널 목록
     */
    suspend fun saveChannels(channels: List<Channel>)
    
    /**
     * 프로젝트 구조를 저장합니다.
     * @param projectId 프로젝트 ID
     * @param projectStructure 프로젝트 구조
     */
    suspend fun saveProjectStructure(projectId: String, projectStructure: ProjectStructure)
    
    /**
     * 카테고리를 삭제합니다.
     * @param categoryId 카테고리 ID
     */
    suspend fun deleteCategory(categoryId: String)
    
    /**
     * 카테고리와 그에 속한 모든 채널을 삭제합니다.
     * @param categoryId 카테고리 ID
     */
    suspend fun deleteCategoryWithChannels(categoryId: String)
    
    /**
     * 채널을 삭제합니다.
     * @param channelId 채널 ID
     */
    suspend fun deleteChannel(channelId: String)
    
    /**
     * 특정 프로젝트의 모든 카테고리와 채널을 삭제합니다.
     * @param projectId 프로젝트 ID
     */
    suspend fun clearProjectStructure(projectId: String)
} 