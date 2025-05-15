package com.example.domain.repository

import com.example.domain.model.Channel
import com.example.domain.model.channel.ProjectChannelRef
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 채널 참조를 관리하는 리포지토리 인터페이스입니다.
 * 채널 자체는 ChannelRepository가 관리하고, 이 인터페이스는 프로젝트와 채널 간의 연결만 관리합니다.
 */
interface ProjectChannelRepository {
    /**
     * 프로젝트에 채널 참조를 추가합니다.
     * @param projectChannelRef 프로젝트 채널 참조 정보
     * @return 성공 시 생성된 참조, 실패 시 예외
     */
    suspend fun addChannelToProject(projectChannelRef: ProjectChannelRef): Result<ProjectChannelRef>
    
    /**
     * 카테고리에 채널 참조를 추가합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @param order 순서 (기본값은 마지막 위치)
     * @return 성공 시 생성된 참조, 실패 시 예외
     */
    suspend fun addChannelToCategory(
        projectId: String,
        categoryId: String,
        channelId: String,
        order: Int = -1
    ): Result<ProjectChannelRef>
    
    /**
     * 프로젝트에 직속 채널 참조를 추가합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param order 순서 (기본값은 마지막 위치)
     * @return 성공 시 생성된 참조, 실패 시 예외
     */
    suspend fun addDirectChannelToProject(
        projectId: String,
        channelId: String,
        order: Int = -1
    ): Result<ProjectChannelRef>
    
    /**
     * 프로젝트 채널 참조를 조회합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 성공 시 참조 정보, 실패 시 예외
     */
    suspend fun getProjectChannelRef(projectId: String, channelId: String): Result<ProjectChannelRef>
    
    /**
     * 프로젝트의 모든 채널 참조를 조회합니다.
     * @param projectId 프로젝트 ID
     * @return 성공 시 참조 목록, 실패 시 예외
     */
    suspend fun getProjectChannelRefs(projectId: String): Result<List<ProjectChannelRef>>
    
    /**
     * 프로젝트의 모든 채널 참조를 실시간으로 구독합니다.
     * @param projectId 프로젝트 ID
     * @return 참조 목록 Flow
     */
    fun getProjectChannelRefsStream(projectId: String): Flow<List<ProjectChannelRef>>
    
    /**
     * 카테고리의 채널 참조를 조회합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @return 성공 시 참조 목록, 실패 시 예외
     */
    suspend fun getCategoryChannelRefs(projectId: String, categoryId: String): Result<List<ProjectChannelRef>>
    
    /**
     * 카테고리의 채널 참조를 실시간으로 구독합니다.
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @return 참조 목록 Flow
     */
    fun getCategoryChannelRefsStream(projectId: String, categoryId: String): Flow<List<ProjectChannelRef>>
    
    /**
     * 프로젝트의 직속 채널 참조를 조회합니다.
     * @param projectId 프로젝트 ID
     * @return 성공 시 참조 목록, 실패 시 예외
     */
    suspend fun getDirectChannelRefs(projectId: String): Result<List<ProjectChannelRef>>
    
    /**
     * 프로젝트의 직속 채널 참조를 실시간으로 구독합니다.
     * @param projectId 프로젝트 ID
     * @return 참조 목록 Flow
     */
    fun getDirectChannelRefsStream(projectId: String): Flow<List<ProjectChannelRef>>
    
    /**
     * 프로젝트 채널 참조를 업데이트합니다 (순서 변경 등).
     * @param projectChannelRef 업데이트할 참조 정보
     * @return 성공 시 Unit, 실패 시 예외
     */
    suspend fun updateProjectChannelRef(projectChannelRef: ProjectChannelRef): Result<Unit>
    
    /**
     * 프로젝트에서 채널 참조를 제거합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 성공 시 Unit, 실패 시 예외
     */
    suspend fun removeChannelFromProject(projectId: String, channelId: String): Result<Unit>
    
    /**
     * 채널 참조의 순서를 변경합니다.
     * @param projectChannelRef 참조 정보
     * @param newOrder 새 순서
     * @return 성공 시 Unit, 실패 시 예외
     */
    suspend fun reorderChannelRef(projectChannelRef: ProjectChannelRef, newOrder: Int): Result<Unit>
    
    /**
     * 프로젝트의 채널 목록을 조회합니다 (참조가 아닌 실제 채널 객체).
     * 
     * 참고: 이 메서드는 ChannelRepository.getProjectChannels로 이전될 예정입니다.
     * ChannelRepository에 통합된 채널 구조에서 이 기능은 중복이지만, 
     * 클라이언트 코드 호환성을 위해 유지합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 성공 시 채널 목록, 실패 시 예외
     */
    suspend fun getProjectChannels(projectId: String): Result<List<Channel>>
    
    /**
     * 프로젝트의 채널 목록을 실시간으로 구독합니다 (참조가 아닌 실제 채널 객체).
     * 
     * 참고: 이 메서드는 ChannelRepository.getProjectChannelsStream으로 이전될 예정입니다.
     * ChannelRepository에 통합된 채널 구조에서 이 기능은 중복이지만, 
     * 클라이언트 코드 호환성을 위해 유지합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 채널 목록 Flow
     */
    fun getProjectChannelsStream(projectId: String): Flow<List<Channel>>
    
    /**
     * 채널 ID가 이미 프로젝트에 존재하는지 확인합니다.
     * 직속 채널 또는 카테고리 내 채널을 모두 검사합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 성공 시 존재 여부, 실패 시 예외
     */
    suspend fun isChannelInProject(projectId: String, channelId: String): Result<Boolean>
    
    /**
     * 채널 참조가 속한 카테고리 ID를 조회합니다.
     * 직속 채널인 경우 null을 반환합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 성공 시 카테고리 ID(또는 null), 실패 시 예외
     */
    suspend fun getChannelCategory(projectId: String, channelId: String): Result<String?>
    
    /**
     * 채널을 한 카테고리에서 다른 카테고리로 이동합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param targetCategoryId 대상 카테고리 ID (null인 경우 직속 채널로 이동)
     * @param order 새 순서 (-1인 경우 마지막 위치)
     * @return 성공 시 업데이트된 참조, 실패 시 예외
     */
    suspend fun moveChannelToCategory(
        projectId: String, 
        channelId: String, 
        targetCategoryId: String?, 
        order: Int = -1
    ): Result<ProjectChannelRef>
    
    /**
     * 특정 채널 ID에 해당하는 모든 프로젝트 참조를 찾습니다.
     * 
     * @param channelId 채널 ID
     * @return 성공 시 해당 채널을 포함하는 모든 프로젝트 참조 목록, 실패 시 예외
     */
    suspend fun findProjectsContainingChannel(channelId: String): Result<List<ProjectChannelRef>>
    
    /**
     * 여러 채널 참조의 순서를 일괄 업데이트합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID (null인 경우 직속 채널)
     * @param reorderedRefs 새로운 순서가 적용된 참조 목록
     * @return 성공 시 Unit, 실패 시 예외
     */
    suspend fun batchReorderChannelRefs(
        projectId: String,
        categoryId: String?,
        reorderedRefs: List<ProjectChannelRef>
    ): Result<Unit>
} 