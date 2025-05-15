package com.example.data.datasource.local.projectstructure

import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ProjectStructure
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 구조(카테고리, 채널) 관련 로컬 데이터 소스 인터페이스.
 * Room 데이터베이스와 상호작용하여 프로젝트 구조 관련 데이터의 로컬 캐시 CRUD 작업을 정의합니다.
 */
interface ProjectStructureLocalDataSource {
    /**
     * 로컬 캐시에서 특정 프로젝트의 구조(카테고리 및 채널 목록)를 조회합니다.
     *
     * @param projectId 조회할 프로젝트의 ID.
     * @return 로컬 캐시에 저장된 [ProjectStructure] 객체. 캐시된 데이터가 없을 경우 빈 구조를 반환할 수 있습니다.
     */
    suspend fun getProjectStructure(projectId: String): ProjectStructure
    
    /**
     * 로컬 캐시에서 특정 프로젝트의 구조(카테고리 및 채널 목록) 변경 사항을 실시간 스트림으로 구독합니다.
     *
     * @param projectId 구독할 프로젝트의 ID.
     * @return 로컬 캐시의 [ProjectStructure] 변경 사항을 방출하는 Flow.
     */
    fun getProjectStructureStream(projectId: String): Flow<ProjectStructure>
    
    /**
     * 로컬 캐시에서 특정 프로젝트에 속한 모든 카테고리 목록을 조회합니다.
     *
     * @param projectId 조회할 프로젝트의 ID.
     * @return 해당 프로젝트의 [Category] 목록. 캐시된 데이터가 없을 경우 빈 목록을 반환합니다.
     */
    suspend fun getCategoriesByProjectId(projectId: String): List<Category>
    
    /**
     * 로컬 캐시에서 특정 프로젝트에 속한 모든 채널 목록을 조회합니다.
     * (카테고리에 속하지 않은 프로젝트 직속 채널 및 모든 카테고리 내 채널 포함)
     *
     * @param projectId 조회할 프로젝트의 ID.
     * @return 해당 프로젝트의 모든 [Channel] 목록. 캐시된 데이터가 없을 경우 빈 목록을 반환합니다.
     */
    suspend fun getChannelsByProjectId(projectId: String): List<Channel>
    
    /**
     * 로컬 캐시에서 특정 카테고리에 속한 모든 채널 목록을 조회합니다.
     *
     * @param categoryId 조회할 카테고리의 ID.
     * @return 해당 카테고리의 [Channel] 목록. 캐시된 데이터가 없을 경우 빈 목록을 반환합니다.
     */
    suspend fun getChannelsByCategoryId(categoryId: String): List<Channel>
    
    /**
     * 로컬 캐시에서 특정 ID를 가진 카테고리 정보를 조회합니다.
     *
     * @param categoryId 조회할 카테고리의 ID.
     * @return 해당 ID의 [Category] 객체. 캐시에 없을 경우 null을 반환합니다.
     */
    suspend fun getCategoryById(categoryId: String): Category?
    
    /**
     * 로컬 캐시에서 특정 ID를 가진 채널 정보를 조회합니다.
     *
     * @param channelId 조회할 채널의 ID.
     * @return 해당 ID의 [Channel] 객체. 캐시에 없을 경우 null을 반환합니다.
     */
    suspend fun getChannelById(channelId: String): Channel?
    
    /**
     * 단일 카테고리 정보를 로컬 캐시에 저장(삽입 또는 업데이트)합니다.
     *
     * @param category 저장할 [Category] 객체.
     */
    suspend fun saveCategory(category: Category)
    
    /**
     * 여러 카테고리 정보를 로컬 캐시에 한 번에 저장(삽입 또는 업데이트)합니다.
     *
     * @param categories 저장할 [Category] 목록.
     */
    suspend fun saveCategories(categories: List<Category>)
    
    /**
     * 단일 채널 정보를 로컬 캐시에 저장(삽입 또는 업데이트)합니다.
     *
     * @param channel 저장할 [Channel] 객체.
     */
    suspend fun saveChannel(channel: Channel)
    
    /**
     * 여러 채널 정보를 로컬 캐시에 한 번에 저장(삽입 또는 업데이트)합니다.
     *
     * @param channels 저장할 [Channel] 목록.
     */
    suspend fun saveChannels(channels: List<Channel>)
    
    /**
     * 특정 프로젝트의 전체 구조(카테고리 및 채널 목록)를 로컬 캐시에 저장합니다.
     * 기존 캐시 데이터는 덮어쓰거나 병합될 수 있습니다.
     *
     * @param projectId 저장할 구조가 속한 프로젝트의 ID.
     * @param projectStructure 저장할 [ProjectStructure] 객체.
     */
    suspend fun saveProjectStructure(projectId: String, projectStructure: ProjectStructure)
    
    /**
     * 로컬 캐시에서 특정 ID를 가진 카테고리를 삭제합니다.
     * 해당 카테고리에 속한 채널은 삭제되지 않습니다.
     *
     * @param categoryId 삭제할 카테고리의 ID.
     */
    suspend fun deleteCategory(categoryId: String)
    
    /**
     * 로컬 캐시에서 특정 카테고리와 그 카테고리에 속한 모든 채널을 함께 삭제합니다.
     *
     * @param categoryId 삭제할 카테고리의 ID.
     */
    suspend fun deleteCategoryWithChannels(categoryId: String)
    
    /**
     * 로컬 캐시에서 특정 ID를 가진 채널을 삭제합니다.
     *
     * @param channelId 삭제할 채널의 ID.
     */
    suspend fun deleteChannel(channelId: String)
    
    /**
     * 로컬 캐시에서 특정 프로젝트에 속한 모든 카테고리와 채널 정보를 삭제합니다.
     *
     * @param projectId 구조를 삭제할 프로젝트의 ID.
     */
    suspend fun clearProjectStructure(projectId: String)
} 