package com.example.data.datasource.local.projectstructure

import com.example.data.db.dao.ProjectStructureDao
import com.example.data.model.local.CategoryEntity
import com.example.data.model.local.ChannelEntity
import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ProjectStructure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 구조(카테고리, 채널) 관련 로컬 데이터 소스 구현체
 * Room 데이터베이스를 사용하여 프로젝트 구조 관련 기능을 구현합니다.
 * 
 * @param projectStructureDao ProjectStructureDao 인스턴스
 */
@Singleton
class ProjectStructureLocalDataSourceImpl @Inject constructor(
    private val projectStructureDao: ProjectStructureDao
) : ProjectStructureLocalDataSource {

    /**
     * 프로젝트 구조를 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조
     */
    override suspend fun getProjectStructure(projectId: String): ProjectStructure {
        val categories = getCategoriesByProjectId(projectId)
        val channels = getChannelsByProjectId(projectId)
        
        return ProjectStructure(categories, channels)
    }

    /**
     * 프로젝트 구조 실시간 스트림을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 Flow
     */
    override fun getProjectStructureStream(projectId: String): Flow<ProjectStructure> {
        val categoriesFlow = projectStructureDao.observeCategoriesByProjectId(projectId)
            .combine(projectStructureDao.observeChannelsByProjectId(projectId)) { categories, channels ->
                val domainCategories = categories.map { it.toDomain() }
                val domainChannels = channels.map { it.toDomain() }
                
                ProjectStructure(domainCategories, domainChannels)
            }
        
        return categoriesFlow
    }

    /**
     * 특정 프로젝트의 모든 카테고리를 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 카테고리 목록
     */
    override suspend fun getCategoriesByProjectId(projectId: String): List<Category> {
        return projectStructureDao.getCategoriesByProjectId(projectId).map { it.toDomain() }
    }

    /**
     * 특정 프로젝트의 모든 채널을 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 채널 목록
     */
    override suspend fun getChannelsByProjectId(projectId: String): List<Channel> {
        return projectStructureDao.getChannelsByProjectId(projectId).map { it.toDomain() }
    }

    /**
     * 특정 카테고리의 모든 채널을 가져옵니다.
     * 
     * @param categoryId 카테고리 ID
     * @return 채널 목록
     */
    override suspend fun getChannelsByCategoryId(categoryId: String): List<Channel> {
        return projectStructureDao.getChannelsByCategoryId(categoryId).map { it.toDomain() }
    }

    /**
     * 카테고리 상세 정보를 가져옵니다.
     * 
     * @param categoryId 카테고리 ID
     * @return 카테고리 또는 null
     */
    override suspend fun getCategoryById(categoryId: String): Category? {
        return projectStructureDao.getCategoryById(categoryId)?.toDomain()
    }

    /**
     * 채널 상세 정보를 가져옵니다.
     * 
     * @param channelId 채널 ID
     * @return 채널 또는 null
     */
    override suspend fun getChannelById(channelId: String): Channel? {
        return projectStructureDao.getChannelById(channelId)?.toDomain()
    }

    /**
     * 카테고리를 저장합니다.
     * 
     * @param category 카테고리
     */
    override suspend fun saveCategory(category: Category) {
        projectStructureDao.insertCategory(CategoryEntity.fromDomain(category))
    }

    /**
     * 카테고리 목록을 저장합니다.
     * 
     * @param categories 카테고리 목록
     */
    override suspend fun saveCategories(categories: List<Category>) {
        projectStructureDao.insertCategories(categories.map { CategoryEntity.fromDomain(it) })
    }

    /**
     * 채널을 저장합니다.
     * 
     * @param channel 채널
     */
    override suspend fun saveChannel(channel: Channel) {
        projectStructureDao.insertChannel(ChannelEntity.fromDomain(channel))
    }

    /**
     * 채널 목록을 저장합니다.
     * 
     * @param channels 채널 목록
     */
    override suspend fun saveChannels(channels: List<Channel>) {
        projectStructureDao.insertChannels(channels.map { ChannelEntity.fromDomain(it) })
    }

    /**
     * 프로젝트 구조를 저장합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param projectStructure 프로젝트 구조
     */
    override suspend fun saveProjectStructure(projectId: String, projectStructure: ProjectStructure) {
        saveCategories(projectStructure.categories)
        saveChannels(projectStructure.channels)
    }

    /**
     * 카테고리를 삭제합니다.
     * 
     * @param categoryId 카테고리 ID
     */
    override suspend fun deleteCategory(categoryId: String) {
        projectStructureDao.deleteCategory(categoryId)
    }

    /**
     * 카테고리와 그에 속한 모든 채널을 삭제합니다.
     * 
     * @param categoryId 카테고리 ID
     */
    override suspend fun deleteCategoryWithChannels(categoryId: String) {
        projectStructureDao.deleteChannelsByCategoryId(categoryId)
        projectStructureDao.deleteCategory(categoryId)
    }

    /**
     * 채널을 삭제합니다.
     * 
     * @param channelId 채널 ID
     */
    override suspend fun deleteChannel(channelId: String) {
        projectStructureDao.deleteChannel(channelId)
    }

    /**
     * 특정 프로젝트의 모든 카테고리와 채널을 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     */
    override suspend fun clearProjectStructure(projectId: String) {
        projectStructureDao.deleteChannelsByProjectId(projectId)
        projectStructureDao.deleteCategoriesByProjectId(projectId)
    }
} 