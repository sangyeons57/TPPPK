package com.example.domain.model.ui

import com.example.domain.model.ChannelType
import com.example.core_common.constants.FirestoreConstants
import com.example.domain.model.ChannelMode

/**
 * UI 상태를 나타내는 데이터 클래스입니다.
 * 프로젝트 상세 화면 (채널 목록 포함)에서 사용됩니다.
 */
data class ProjectDetailUiState(
    val projectId: String,
    val projectName: String = "", // TODO: 프로젝트 이름도 가져오도록 수정
    val categories: List<CategoryUiModel> = emptyList(),
    val directChannels: List<ChannelUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // 채널 생성 관련 상태
    val showCreateChannelDialog: Boolean = false,
    val createChannelDialogData: CreateChannelDialogData? = null // categoryId가 null이면 직속 채널
)

/**
 * 채널 생성 다이얼로그 관련 데이터를 담는 클래스입니다.
 */
data class CreateChannelDialogData(
    val categoryId: String?, // null이면 직속 채널
    val channelName: String = "",
    val channelMode: ChannelMode = ChannelMode.TEXT
)

// Added/Updated UI Models
data class ChannelUiModel(
    val id: String,
    val categoryId: String?,
    val projectId: String,
    val name: String,
    val type: ChannelType, 
    val order: Int,
    val isDirect: Boolean,
    val lastMessagePreview: String? = null,
    val formattedLastMessageTimestamp: String? = null
)

data class CategoryUiModel(
    val id: String,
    val projectId: String,
    val name: String,
    val order: Int,
    val channels: List<ChannelUiModel> = emptyList(),
    val formattedCreatedAt: String? = null,
    val formattedUpdatedAt: String? = null
)