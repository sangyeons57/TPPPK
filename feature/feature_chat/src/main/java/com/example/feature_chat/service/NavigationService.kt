package com.example.feature_chat.service

import android.util.Log
import com.example.core_navigation.core.NavigationManger

/**
 * 네비게이션 처리를 담당하는 Service
 * NavigationManager를 래핑하여 채팅 화면에 특화된 네비게이션 기능을 제공합니다.
 */
class NavigationService(
    private val navigationManager: NavigationManger
) {
    
    /**
     * 뒤로 가기 처리
     */
    suspend fun navigateBack() {
        Log.d("NavigationService", "Navigating back from chat screen")
        navigationManager.navigateBack()
    }
    
    /**
     * 사용자 프로필 화면으로 이동
     */
    suspend fun navigateToUserProfile(userId: String) {
        Log.d("NavigationService", "Navigating to user profile: $userId")
        // 실제 구현은 NavigationManager에 해당 메서드가 있는지 확인 필요
        // navigationManager.navigateToUserProfile(userId)
    }
    
    /**
     * 채팅 설정 화면으로 이동
     */
    suspend fun navigateToChatSettings(channelId: String) {
        Log.d("NavigationService", "Navigating to chat settings: $channelId")
        // 실제 구현은 NavigationManager에 해당 메서드가 있는지 확인 필요
        // navigationManager.navigateToChatSettings(channelId)
    }
    
    /**
     * 이미지 갤러리 화면으로 이동
     */
    suspend fun navigateToImageGallery(imageUrls: List<String>, initialIndex: Int = 0) {
        Log.d("NavigationService", "Navigating to image gallery with ${imageUrls.size} images")
        // 실제 구현은 NavigationManager에 해당 메서드가 있는지 확인 필요
        // navigationManager.navigateToImageGallery(imageUrls, initialIndex)
    }
    
    /**
     * 파일 첨부 화면으로 이동
     */
    suspend fun navigateToFileAttachment() {
        Log.d("NavigationService", "Navigating to file attachment")
        // 실제 구현은 NavigationManager에 해당 메서드가 있는지 확인 필요
        // navigationManager.navigateToFileAttachment()
    }
    
    /**
     * 멘션된 사용자 목록 화면으로 이동
     */
    suspend fun navigateToMentionedUsers(channelId: String) {
        Log.d("NavigationService", "Navigating to mentioned users for channel: $channelId")
        // 실제 구현은 NavigationManager에 해당 메서드가 있는지 확인 필요
        // navigationManager.navigateToMentionedUsers(channelId)
    }
    
    /**
     * 검색 화면으로 이동
     */
    suspend fun navigateToSearch(channelId: String) {
        Log.d("NavigationService", "Navigating to search for channel: $channelId")
        // 실제 구현은 NavigationManager에 해당 메서드가 있는지 확인 필요
        // navigationManager.navigateToSearch(channelId)
    }
}