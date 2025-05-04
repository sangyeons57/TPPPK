package com.example.data.datasource.remote.projectstructure

import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.example.domain.model.ProjectStructure
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 구조(카테고리, 채널) 관련 원격 데이터 소스 인터페이스
 * 프로젝트 구조 관련 Firebase Firestore 작업을 정의합니다.
 */
interface ProjectStructureRemoteDataSource {
    /**
     * 프로젝트 구조를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조(카테고리, 채널 목록) 결과
     */
    suspend fun getProjectStructure(projectId: String): Result<ProjectStructure>
    
    /**
     * 프로젝트 구조 실시간 스트림을 가져옵니다.
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
     * 새 채널을 생성합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param name 채널 이름
     * @param type 채널 타입
     * @return 생성된 채널 결과
     */
    suspend fun createChannel(projectId: String, categoryId: String, name: String, type: ChannelType): Result<Channel>
    
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
     * @param newName 새 카테고리 이름
     * @return 작업 성공 여부
     */
    suspend fun updateCategory(projectId: String, categoryId: String, newName: String): Result<Unit>
    
    /**
     * 카테고리를 삭제합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @return 작업 성공 여부
     */
    suspend fun deleteCategory(projectId: String, categoryId: String): Result<Unit>
    
    /**
     * 채널 상세 정보를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @return 채널 상세 정보 결과
     */
    suspend fun getChannelDetails(projectId: String, categoryId: String, channelId: String): Result<Channel>
    
    /**
     * 채널 정보를 수정합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @param newName 새 채널 이름
     * @param newType 새 채널 타입
     * @return 작업 성공 여부
     */
    suspend fun updateChannel(projectId: String, categoryId: String, channelId: String, newName: String, newType: ChannelType): Result<Unit>
    
    /**
     * 채널을 삭제합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @return 작업 성공 여부
     */
    suspend fun deleteChannel(projectId: String, categoryId: String, channelId: String): Result<Unit>
} 