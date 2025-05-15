package com.example.data.datasource.remote.projectstructure

import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ChannelMode
import com.example.domain.model.ProjectStructure
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 구조(카테고리, 채널) 관련 원격 데이터 소스 인터페이스
 * 프로젝트 구조 관련 Firebase Firestore 작업을 정의합니다.
 */
interface ProjectStructureRemoteDataSource {
    /**
     * 프로젝트 구조(카테고리 및 직속 채널 포함)를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 결과
     */
    suspend fun getProjectStructure(projectId: String): Result<ProjectStructure>
    
    /**
     * 프로젝트 구조(카테고리 및 직속 채널 포함) 실시간 스트림을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 Flow
     */
    fun getProjectStructureStream(projectId: String): Flow<ProjectStructure>
    
    /**
     * 새 카테고리를 생성합니다.
     * @param projectId 프로젝트 ID
     * @param name 카테고리 이름
     * @return 생성된 카테고리 결과
     */
    suspend fun createCategory(projectId: String, name: String): Result<Category>

    /**
     * 카테고리 상세 정보를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @return 카테고리 상세 정보 결과
     */
    suspend fun getCategoryDetails(projectId: String, categoryId: String): Result<Category>
    
    /**
     * 카테고리 정보를 수정합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param newName 새 카테고리 이름 (nullable)
     * @param newOrder 새 카테고리 순서 (nullable)
     * @return 작업 성공 여부
     */
    suspend fun updateCategory(projectId: String, categoryId: String, newName: String?, newOrder: Int?): Result<Unit>
    
    /**
     * 카테고리를 삭제합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @return 작업 성공 여부
     */
    suspend fun deleteCategory(projectId: String, categoryId: String): Result<Unit>
    
    /**
     * 카테고리 내 새 채널을 생성합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param name 채널 이름
     * @param channelMode 채널 모드 (e.g., "TEXT", "VOICE")
     * @param order 채널 순서 (Optional)
     * @return 생성된 채널 결과
     */
    suspend fun createCategoryChannel(projectId: String, categoryId: String, name: String, channelMode: ChannelMode, order: Int? = null): Result<Channel>
    
    /**
     * 카테고리 내 채널 상세 정보를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @return 채널 상세 정보 결과
     */
    suspend fun getCategoryChannelDetails(projectId: String, categoryId: String, channelId: String): Result<Channel>
    
    /**
     * 카테고리 내 채널 정보를 수정합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @param newName 새 채널 이름
     * @param newChannelMode 새 채널 모드
     * @return 작업 성공 여부
     */
    suspend fun updateCategoryChannel(projectId: String, categoryId: String, channelId: String, newName: String, newChannelMode: ChannelMode): Result<Unit>
    
    /**
     * 카테고리 내 채널을 삭제합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @return 작업 성공 여부
     */
    suspend fun deleteCategoryChannel(projectId: String, categoryId: String, channelId: String): Result<Unit>

    // --- Project-Level Channel Management ---

    /**
     * 프로젝트 직속 새 채널을 생성합니다. (카테고리에 속하지 않음)
     * @param projectId 프로젝트 ID
     * @param name 채널 이름
     * @param channelMode 채널 모드 (e.g., "TEXT", "VOICE")
     * @param order 채널 순서 (Optional)
     * @return 생성된 채널 결과
     */
    suspend fun createProjectChannel(projectId: String, name: String, channelMode: ChannelMode, order: Int? = null): Result<Channel>

    /**
     * 프로젝트 직속 채널 상세 정보를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 채널 상세 정보 결과
     */
    suspend fun getProjectChannelDetails(projectId: String, channelId: String): Result<Channel>

    /**
     * 프로젝트 직속 채널 정보를 수정합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param newName 새 채널 이름
     * @param newChannelMode 새 채널 모드
     * @return 작업 성공 여부
     */
    suspend fun updateProjectChannel(projectId: String, channelId: String, newName: String, newChannelMode: ChannelMode): Result<Unit>

    /**
     * 프로젝트 직속 채널을 삭제합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 작업 성공 여부
     */
    suspend fun deleteProjectChannel(projectId: String, channelId: String): Result<Unit>
} 