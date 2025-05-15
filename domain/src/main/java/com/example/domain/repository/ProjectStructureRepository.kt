// 경로: domain/repository/ProjectStructureRepository.kt (신규 생성)
package com.example.domain.repository

import com.example.domain.model.Category
import com.example.domain.model.ProjectStructure
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 프로젝트 구조(카테고리 등) 관리를 위한 리포지토리 인터페이스입니다.
 * 이 인터페이스는 프로젝트의 카테고리 관리에 중점을 두며,
 * 채널 관리는 ChannelRepository가, 채널-프로젝트 연결은 ProjectChannelRepository가 담당합니다.
 */
interface ProjectStructureRepository {
    /**
     * 프로젝트 구조를 실시간으로 조회합니다.
     * 카테고리, 직속 채널, 카테고리별 채널을 포함한 전체 구조를 제공합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 Flow
     */
    fun getProjectStructureStream(projectId: String): Flow<ProjectStructure>
    
    /**
     * 프로젝트 구조를 한 번 조회합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 결과
     */
    suspend fun getProjectStructure(projectId: String): Result<ProjectStructure>
    
    /**
     * 카테고리를 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param name 카테고리 이름
     * @return 생성된 카테고리 결과
     */
    suspend fun createCategory(projectId: String, name: String): Result<Category>
    
    /**
     * 카테고리 상세 정보를 조회합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @return 카테고리 상세 정보 결과
     */
    suspend fun getCategoryDetails(projectId: String, categoryId: String): Result<Category>
    
    /**
     * 프로젝트의 모든 카테고리를 조회합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 카테고리 목록 결과
     */
    suspend fun getProjectCategories(projectId: String): Result<List<Category>>
    
    /**
     * 프로젝트의 모든 카테고리를 실시간으로 구독합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 카테고리 목록 Flow
     */
    fun getProjectCategoriesStream(projectId: String): Flow<List<Category>>
    
    /**
     * 카테고리 정보를 수정합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param newName 새 카테고리 이름
     * @return 작업 결과
     */
    suspend fun updateCategory(projectId: String, categoryId: String, newName: String, order: Int?): Result<Unit>
    
    /**
     * 카테고리를 삭제합니다.
     * 이 작업은 카테고리에 속한 채널의 참조만 제거하며 채널 자체는 삭제하지 않습니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param deleteChannels true인 경우 카테고리에 속한 채널도 함께 삭제, false인 경우 채널은 유지
     * @return 작업 결과
     */
    suspend fun deleteCategory(projectId: String, categoryId: String, deleteChannels: Boolean = false): Result<Unit>
    
    /**
     * 카테고리 순서를 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param newOrder 새 순서 (0부터 시작)
     * @return 작업 결과
     */
    suspend fun reorderCategory(projectId: String, categoryId: String, newOrder: Int): Result<Unit>
    
    /**
     * 여러 카테고리의 순서를 일괄 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryOrders 카테고리 ID와 순서 쌍의 맵
     * @return 작업 결과
     */
    suspend fun batchReorderCategories(projectId: String, categoryOrders: Map<String, Int>): Result<Unit>
}