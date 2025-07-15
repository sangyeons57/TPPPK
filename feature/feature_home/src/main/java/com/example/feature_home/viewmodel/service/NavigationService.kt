package com.example.feature_home.viewmodel.service

import android.util.Log
import com.example.core_navigation.core.NavigationManger
import com.example.domain.model.vo.DocumentId
import com.example.feature_home.model.CategoryUiModel
import com.example.feature_home.model.ChannelUiModel
import com.example.feature_home.model.DmUiModel

/**
 * 네비게이션 처리를 담당하는 Service
 * NavigationManager를 래핑하여 UI에 특화된 네비게이션 기능을 제공합니다.
 */
class NavigationService(
    private val navigationManager: NavigationManger
) {
    
    /**
     * 프로젝트 설정 화면으로 이동
     */
    fun navigateToProjectSettings(projectId: DocumentId) {
        Log.d("NavigationService", "Navigating to project settings: $projectId")
        navigationManager.navigateToProjectSettings(projectId.value)
    }
    
    /**
     * DM 채팅 화면으로 이동
     */
    fun navigateToDmChat(dmId: DocumentId) {
        Log.d("NavigationService", "Navigating to DM chat: $dmId")
        navigationManager.navigateToChat(dmId.value)
    }
    
    /**
     * 채널 화면으로 이동
     */
    fun navigateToChannel(projectId: DocumentId, channelId: DocumentId) {
        Log.d("NavigationService", "Navigating to channel: $channelId in project: $projectId")
        when {
            // 태스크 채널인 경우 태스크 리스트로 이동
            isTaskChannel(channelId) -> {
                navigationManager.navigateToTaskList(projectId.value, channelId.value)
            }
            // 일반 채널인 경우 채팅으로 이동
            else -> {
                navigationManager.navigateToChat(channelId.value)
            }
        }
    }
    
    /**
     * 태스크 리스트 화면으로 이동
     */
    fun navigateToTaskList(projectId: DocumentId, channelId: DocumentId) {
        Log.d("NavigationService", "Navigating to task list: $channelId in project: $projectId")
        navigationManager.navigateToTaskList(projectId.value, channelId.value)
    }
    
    /**
     * 프로젝트 추가 화면으로 이동
     */
    fun navigateToAddProject() {
        Log.d("NavigationService", "Navigating to add project")
        navigationManager.navigateToAddProject()
    }
    
    /**
     * 카테고리 순서 변경 화면으로 이동
     */
    fun navigateToReorderCategory(projectId: DocumentId) {
        Log.d("NavigationService", "Navigating to reorder category: $projectId")
        // 실제로는 다이얼로그로 처리되므로 별도 네비게이션 없음
    }
    
    /**
     * 채널 순서 변경 화면으로 이동
     */
    fun navigateToReorderChannel(projectId: DocumentId, categoryId: DocumentId?) {
        Log.d("NavigationService", "Navigating to reorder channel: $projectId, category: $categoryId")
        // 실제로는 다이얼로그로 처리되므로 별도 네비게이션 없음
    }
    
    /**
     * 채널 클릭 처리 - 채널 타입에 따라 다른 네비게이션 수행
     */
    fun handleChannelClick(projectId: DocumentId, channel: ChannelUiModel) {
        Log.d("NavigationService", "Handling channel click: ${channel.name} in project: $projectId")
        
        // 채널 타입에 따라 다른 네비게이션 처리
        when (channel.mode) {
            com.example.domain.model.enum.ProjectChannelType.TASKS -> {
                navigateToTaskList(projectId, channel.id)
            }
            com.example.domain.model.enum.ProjectChannelType.MESSAGES -> {
                navigateToChannel(projectId, channel.id)
            }
            else -> {
                navigateToChannel(projectId, channel.id)
            }
        }
    }
    
    /**
     * DM 아이템 클릭 처리
     */
    fun handleDmItemClick(dm: DmUiModel) {
        Log.d("NavigationService", "Handling DM item click: ${dm.partnerName}")
        navigateToDmChat(dm.channelId)
    }
    
    /**
     * 프로젝트 설정 클릭 처리
     */
    fun handleProjectSettingsClick(projectId: DocumentId) {
        Log.d("NavigationService", "Handling project settings click: $projectId")
        navigateToProjectSettings(projectId)
    }
    
    /**
     * 채널이 태스크 채널인지 확인
     */
    private fun isTaskChannel(channelId: DocumentId): Boolean {
        // 실제로는 채널 타입을 확인하는 로직이 필요
        // 임시로 false 반환
        return false
    }
}