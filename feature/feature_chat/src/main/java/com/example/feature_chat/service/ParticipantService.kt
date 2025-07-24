package com.example.feature_chat.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.provider.dm.DMUseCaseProvider
import com.example.feature_chat.model.ChatParticipant

/**
 * DM 채널의 참가자 관리를 담당하는 Service
 * DM 채널의 참가자 정보 로딩 및 관리를 담당합니다.
 */
class ParticipantService(
    private val dmUseCaseProvider: DMUseCaseProvider,
    private val channelId: String,
    private val userProfileService: UserProfileService
) {
    
    private val tag = "ParticipantService"
    
    /**
     * DM 채널의 참가자 목록을 로딩합니다.
     */
    suspend fun loadParticipants(): List<ChatParticipant> {
        Log.d(tag, "loadParticipants: Loading participants for DM channel $channelId")
        
        return try {
            // Get current user's DM channel UseCases
            // Note: We need to get the current user ID properly
            val currentUserId = getCurrentUserId()
            val dmUseCases = dmUseCaseProvider.createForUser(UserId(currentUserId))
            
            // Use GetDmChannelUseCase to get DM channel details
            Log.d("ParticipantService", "channelId: $channelId")
            when (val result = dmUseCases.getDmChannelUseCase(DocumentId(channelId))) {
                is CustomResult.Success -> {
                    val dmChannel = result.data
                    Log.d(tag, "loadParticipants: Successfully loaded DM channel with ${dmChannel.participants.size} participants")
                    
                    // Convert participants to ChatParticipant UI models
                    val participants = dmChannel.participants.map { userId ->
                        // Load user profile to get real display name
                        userProfileService.loadUserProfile(userId.value)
                        
                        val displayName = userProfileService.getUserDisplayName(userId.value)
                        val profileUrl = userProfileService.getCachedProfileUrl(userId.value)
                        
                        ChatParticipant(
                            userId = userId.value,
                            displayName = displayName,
                            profileUrl = profileUrl,
                            isOnline = false // We don't have online status for now
                        )
                    }
                    
                    Log.d(tag, "loadParticipants: Converted ${participants.size} participants to UI models")
                    participants
                }
                is CustomResult.Failure -> {
                    Log.e(tag, "loadParticipants: Failed to load DM channel", result.error)
                    emptyList()
                }
                else -> {
                    Log.w(tag, "loadParticipants: Unexpected result type: ${result::class.simpleName}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "loadParticipants: Exception while loading participants", e)
            emptyList()
        }
    }
    
    /**
     * 현재 사용자 ID를 가져옵니다.
     * TODO: AuthService에서 실제 현재 사용자 ID를 가져오도록 구현 필요
     */
    private suspend fun getCurrentUserId(): String {
        // TODO: Get from AuthService or similar
        return "current_user_id" // Placeholder - needs real implementation
    }
    
    /**
     * 참가자의 온라인 상태를 업데이트합니다.
     * TODO: 실제 온라인 상태 추적 구현 필요
     */
    suspend fun updateParticipantOnlineStatus(userId: String, isOnline: Boolean) {
        Log.d(tag, "updateParticipantOnlineStatus: Updating online status for user $userId to $isOnline")
        // This would be implemented when we have real-time presence system
    }
    
    /**
     * 특정 참가자의 정보를 새로고침합니다.
     */
    suspend fun refreshParticipant(userId: String): ChatParticipant? {
        Log.d(tag, "refreshParticipant: Refreshing participant info for user $userId")
        
        return try {
            // Load fresh user profile
            userProfileService.loadUserProfile(userId)
            
            val displayName = userProfileService.getUserDisplayName(userId)
            val profileUrl = userProfileService.getCachedProfileUrl(userId)
            
            ChatParticipant(
                userId = userId,
                displayName = displayName,
                profileUrl = profileUrl,
                isOnline = false
            )
        } catch (e: Exception) {
            Log.e(tag, "refreshParticipant: Exception while refreshing participant $userId", e)
            null
        }
    }
}