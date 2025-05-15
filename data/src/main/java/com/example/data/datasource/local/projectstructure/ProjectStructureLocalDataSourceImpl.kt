package com.example.data.datasource.local.projectstructure

import com.example.data.db.dao.ProjectStructureDao
import com.example.data.model.mapper.CategoryMapper
import com.example.data.model.mapper.ChannelMapper
import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ProjectStructure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.example.data.model.local.CategoryEntity
import com.example.data.model.local.ChannelEntity

/**
 * Room 데이터베이스를 사용하여 프로젝트 구조(카테고리, 채널) 데이터를 로컬에서 관리하는 데이터 소스 구현체
 */
class ProjectStructureLocalDataSourceImpl @Inject constructor(
    private val projectStructureDao: ProjectStructureDao,
    private val categoryMapper: CategoryMapper,
    private val channelMapper: ChannelMapper
) : ProjectStructureLocalDataSource {

    /**
     * 로컬 캐시에서 특정 프로젝트의 구조(카테고리 및 채널 목록)를 조회합니다.
     * 캐시된 데이터가 없을 경우 빈 구조를 반환합니다.
     */
    override suspend fun getProjectStructure(projectId: String): ProjectStructure {
        val categoryEntities = projectStructureDao.getCategoriesByProjectId(projectId)
        val channelEntities = projectStructureDao.getChannelsByProjectId(projectId)

        val allCategories: List<Category> = categoryEntities.map { entity: CategoryEntity -> categoryMapper.toDomain(entity) }
        val allChannels: List<Channel> = channelEntities.map { entity: ChannelEntity -> channelMapper.toDomain(entity) }

        // 카테고리별 채널 그룹화
        val categoriesWithChannels = allCategories.map { category: Category ->
            category.copy(channels = allChannels.filter { channel: Channel -> channel.projectSpecificData?.categoryId == category.id })
        }
        // 프로젝트 직속 채널 필터링
        val projectDirectChannels = allChannels.filter { channel: Channel -> channel.projectSpecificData?.categoryId == null && channel.projectSpecificData?.projectId == projectId }

        return ProjectStructure(
            categories = categoriesWithChannels,
            directChannels = projectDirectChannels
        )
    }

    /**
     * 로컬 캐시에서 특정 프로젝트의 구조 변경 사항을 실시간 스트림으로 구독합니다.
     */
    override fun getProjectStructureStream(projectId: String): Flow<ProjectStructure> {
        val categoriesFlow: Flow<List<CategoryEntity>> = projectStructureDao.observeCategoriesByProjectId(projectId)
        val channelsFlow: Flow<List<ChannelEntity>> = projectStructureDao.observeChannelsByProjectId(projectId)

        return combine(categoriesFlow, channelsFlow) { categoryEntities: List<CategoryEntity>, channelEntities: List<ChannelEntity> ->
            val allCategories: List<Category> = categoryEntities.map { entity: CategoryEntity -> categoryMapper.toDomain(entity) }
            val allChannels: List<Channel> = channelEntities.map { entity: ChannelEntity -> channelMapper.toDomain(entity) }

            val categoriesWithChannels = allCategories.map { category: Category ->
                category.copy(channels = allChannels.filter { channel: Channel -> channel.projectSpecificData?.categoryId == category.id })
            }
            val projectDirectChannels = allChannels.filter { channel: Channel -> channel.projectSpecificData?.categoryId == null && channel.projectSpecificData?.projectId == projectId }

            ProjectStructure(
                categories = categoriesWithChannels,
                directChannels = projectDirectChannels
            )
        }
    }

    /**
     * 로컬 캐시에서 특정 프로젝트에 속한 모든 카테고리 목록을 조회합니다.
     */
    override suspend fun getCategoriesByProjectId(projectId: String): List<Category> {
        return projectStructureDao.getCategoriesByProjectId(projectId).map { entity: CategoryEntity -> categoryMapper.toDomain(entity) }
    }

    /**
     * 로컬 캐시에서 특정 프로젝트에 속한 모든 채널 목록을 조회합니다.
     */
    override suspend fun getChannelsByProjectId(projectId: String): List<Channel> {
        return projectStructureDao.getChannelsByProjectId(projectId).map { entity: ChannelEntity -> channelMapper.toDomain(entity) }
    }

    /**
     * 로컬 캐시에서 특정 카테고리에 속한 모든 채널 목록을 조회합니다.
     */
    override suspend fun getChannelsByCategoryId(categoryId: String): List<Channel> {
        return projectStructureDao.getChannelsByCategoryId(categoryId).map { entity: ChannelEntity -> channelMapper.toDomain(entity) }
    }

    /**
     * 로컬 캐시에서 특정 ID를 가진 카테고리 정보를 조회합니다.
     */
    override suspend fun getCategoryById(categoryId: String): Category? {
        val entity = projectStructureDao.getCategoryById(categoryId)
        return entity?.let { entity: CategoryEntity -> categoryMapper.toDomain(entity) }
    }

    /**
     * 로컬 캐시에서 특정 ID를 가진 채널 정보를 조회합니다.
     */
    override suspend fun getChannelById(channelId: String): Channel? {
        val entity: ChannelEntity? = projectStructureDao.getChannelById(channelId)
        return entity?.let { channelMapper.toDomain(it) }
    }

    /**
     * 단일 카테고리 정보를 로컬 캐시에 저장(삽입 또는 업데이트)합니다.
     */
    override suspend fun saveCategory(category: Category) {
        projectStructureDao.insertCategory(categoryMapper.toEntity(category))
    }

    /**
     * 여러 카테고리 정보를 로컬 캐시에 한 번에 저장(삽입 또는 업데이트)합니다.
     */
    override suspend fun saveCategories(categories: List<Category>) {
        projectStructureDao.insertCategories(categories.map { category: Category -> categoryMapper.toEntity(category) })
    }

    /**
     * 단일 채널 정보를 로컬 캐시에 저장(삽입 또는 업데이트)합니다.
     */
    override suspend fun saveChannel(channel: Channel) {
        projectStructureDao.insertChannel(channelMapper.toEntity(channel))
    }

    /**
     * 여러 채널 정보를 로컬 캐시에 한 번에 저장(삽입 또는 업데이트)합니다.
     */
    override suspend fun saveChannels(channels: List<Channel>) {
        projectStructureDao.insertChannels(channels.map { channel: Channel -> channelMapper.toEntity(channel) })
    }

    /**
     * 특정 프로젝트의 전체 구조(카테고리 및 채널 목록)를 로컬 캐시에 저장합니다.
     * 기존 캐시 데이터는 삭제 후 새로 삽입.
     */
    override suspend fun saveProjectStructure(projectId: String, projectStructure: ProjectStructure) {
        projectStructureDao.deleteCategoriesByProjectId(projectId)
        projectStructureDao.deleteChannelsByProjectId(projectId)

        val categoryEntities: List<CategoryEntity> = projectStructure.categories.map { category: Category -> categoryMapper.toEntity(category) }
        if (categoryEntities.isNotEmpty()) {
            projectStructureDao.insertCategories(categoryEntities)
        }

        // Save all channels (direct and those within categories)
        val allChannels: List<Channel> = projectStructure.directChannels + projectStructure.categories.flatMap { category: Category -> category.channels }
        val channelEntities: List<ChannelEntity> = allChannels.map { channel: Channel -> channelMapper.toEntity(channel) }
        if (channelEntities.isNotEmpty()) {
            projectStructureDao.insertChannels(channelEntities)
        }
    }

    /**
     * 로컬 캐시에서 특정 ID를 가진 카테고리를 삭제합니다.
     */
    override suspend fun deleteCategory(categoryId: String) {
        projectStructureDao.deleteCategory(categoryId)
        // Note: Interface says this does NOT delete associated channels.
    }

    /**
     * 로컬 캐시에서 특정 카테고리와 그 카테고리에 속한 모든 채널을 함께 삭제합니다.
     */
    override suspend fun deleteCategoryWithChannels(categoryId: String) {
        projectStructureDao.deleteChannelsByCategoryId(categoryId)
        projectStructureDao.deleteCategory(categoryId)
    }

    /**
     * 로컬 캐시에서 특정 ID를 가진 채널을 삭제합니다.
     */
    override suspend fun deleteChannel(channelId: String) {
        projectStructureDao.deleteChannel(channelId)
    }

    /**
     * 로컬 캐시에서 특정 프로젝트에 속한 모든 카테고리와 채널 정보를 삭제합니다.
     */
    override suspend fun clearProjectStructure(projectId: String) {
        projectStructureDao.deleteChannelsByProjectId(projectId)
        projectStructureDao.deleteCategoriesByProjectId(projectId)
    }

    // Removed methods not in interface: getCategoryDetails, getChannelDetails, observeProjectStructure, syncProjectData, clearProjectData
} 