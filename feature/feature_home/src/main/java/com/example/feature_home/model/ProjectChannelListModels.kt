package com.example.feature_home.model

import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.enum.ProjectChannelType


/**
 * 프로젝트 상세 화면의 카테고리 및 채널 목록 표시를 위한 UI 모델
 */
data class ProjectStructureUiState(
    val categories: List<CategoryUiModel> = emptyList(),
    val directChannel: List<ChannelUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedChannelId: String? = null
)


/**
 * 카테고리 UI 모델
 * @param id 카테고리 ID
 * @param name 카테고리 이름
 * @param channels 카테고리에 속한 채널 목록
 * @param isExpanded 카테고리 펼침 상태
 */
data class CategoryUiModel(
    val id: String,
    val name: String,
    val channels: List<ChannelUiModel> = emptyList(),
    val isExpanded: Boolean = true
) {
    companion object {
        fun fromDomain(category: Category, isExpanded: Boolean = true): CategoryUiModel {
            return CategoryUiModel(
                id = category.id,
                name = category.name,
                isExpanded = isExpanded
            )
        }
    }
}

/**
 * 채널 UI 모델
 * @param id 채널 ID
 * @param name 채널 이름
 * @param mode 채널 타입 (TEXT, VOICE)
 * @param isSelected 채널 선택 상태
 */
data class ChannelUiModel(
    val id: String,
    val name: String,
    val mode: ProjectChannelType,
    val isSelected: Boolean = false
) {
    companion object {
        fun fromDomain(channel: ProjectChannel): ChannelUiModel {
            return ChannelUiModel(
                id = channel.id,
                name = channel.channelName,
                mode = channel.channelType
            )
        }
    }
} 