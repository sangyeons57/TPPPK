package com.example.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 및 채널 참가자를 관리하는 리포지토리 인터페이스
 * 새로운 채널 구조에서 프로젝트/카테고리 채널의 참가자는 이 인터페이스를 통해 관리
 */
interface ProjectParticipantRepository {
    /**
     * 프로젝트에 참가자를 추가합니다.
     * @param projectId 프로젝트 ID
     * @param userId 추가할 사용자 ID
     * @return 성공 시 Unit, 실패 시 예외
     */
    suspend fun addProjectParticipant(projectId: String, userId: String): Result<Unit>
    
    /**
     * 프로젝트에서 참가자를 제거합니다.
     * @param projectId 프로젝트 ID
     * @param userId 제거할 사용자 ID
     * @return 성공 시 Unit, 실패 시 예외
     */
    suspend fun removeProjectParticipant(projectId: String, userId: String): Result<Unit>
    
    /**
     * 채널에 참가자를 추가합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param userId 추가할 사용자 ID
     * @return 성공 시 Unit, 실패 시 예외
     */
    suspend fun addChannelParticipant(projectId: String, channelId: String, userId: String): Result<Unit>
    
    /**
     * 채널에서 참가자를 제거합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param userId 제거할 사용자 ID
     * @return 성공 시 Unit, 실패 시 예외
     */
    suspend fun removeChannelParticipant(projectId: String, channelId: String, userId: String): Result<Unit>
    
    /**
     * 프로젝트의 참가자 목록을 조회합니다.
     * @param projectId 프로젝트 ID
     * @return 성공 시 참가자 ID 목록, 실패 시 예외
     */
    suspend fun getProjectParticipants(projectId: String): Result<List<String>>
    
    /**
     * 프로젝트의 참가자 목록을 실시간으로 구독합니다.
     * @param projectId 프로젝트 ID
     * @return 참가자 ID 목록 Flow
     */
    fun getProjectParticipantsStream(projectId: String): Flow<List<String>>
    
    /**
     * 채널의 참가자 목록을 조회합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 성공 시 참가자 ID 목록, 실패 시 예외
     */
    suspend fun getChannelParticipants(projectId: String, channelId: String): Result<List<String>>
    
    /**
     * 채널의 참가자 목록을 실시간으로 구독합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 참가자 ID 목록 Flow
     */
    fun getChannelParticipantsStream(projectId: String, channelId: String): Flow<List<String>>
    
    /**
     * 사용자가 채널에 참가 중인지 확인합니다.
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @param userId 확인할 사용자 ID
     * @return 성공 시 참가 여부, 실패 시 예외
     */
    suspend fun isChannelParticipant(projectId: String, channelId: String, userId: String): Result<Boolean>
} 