package com.example.feature_home.dialog.viewmodel

import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.enum.ProjectChannelType

/**
 * AddProjectElementDialog의 UI 상태
 */
data class AddProjectElementDialogUiState(
    // 공통 상태
    val isLoading: Boolean = false,
    val selectedTab: CreateElementType = CreateElementType.CATEGORY,
    
    // 카테고리 생성 관련
    val categoryName: String = "",
    val categoryNameError: String? = null,
    
    // 채널 생성 관련
    val channelName: String = "",
    val channelNameError: String? = null,
    val selectedCategoryId: String? = null,
    val selectedChannelType: ProjectChannelType = ProjectChannelType.MESSAGES,
    val availableCategories: List<Category> = emptyList()
)

/**
 * 생성할 요소 타입
 */
enum class CreateElementType {
    CATEGORY, // 카테고리
    CHANNEL   // 채널
}

/**
 * AddProjectElementDialog 이벤트
 */
sealed interface AddProjectElementDialogEvent {
    /**
     * 다이얼로그 닫기
     */
    data object DismissDialog : AddProjectElementDialogEvent
    
    /**
     * 카테고리 생성 완료
     */
    data class CategoryCreated(val category: Category) : AddProjectElementDialogEvent
    
    /**
     * 채널 생성 완료
     */
    data class ChannelCreated(val channel: ProjectChannel) : AddProjectElementDialogEvent
}