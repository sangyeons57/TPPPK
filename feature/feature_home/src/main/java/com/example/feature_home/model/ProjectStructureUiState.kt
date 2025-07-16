package com.example.feature_home.model

import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.usecase.project.structure.ProjectStructureData
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.category.CategoryName


/**
 * 프로젝트 상세 화면의 카테고리 및 채널 목록 표시를 위한 UI 모델
 * 통합된 구조를 사용하여 카테고리와 채널을 관리합니다.
 */
data class ProjectStructureUiState(
    val categories: List<CategoryUiModel> = emptyList(),
    val directChannel: List<ChannelUiModel> = emptyList(),
    val unifiedStructureItems: List<ProjectStructureItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedChannelId: String? = null
) {

    companion object {
        /**
         * ProjectStructureData를 UI 모델로 변환합니다.
         */
        fun fromDomain(
            data: ProjectStructureData,
            expandedCategoryIds: Set<String> = emptySet(),
            selectedChannelId: String? = null
        ): ProjectStructureUiState {
            val categoryUiModels = data.categoryChannelMap.map { (category, channels) ->
                CategoryUiModel(
                    id = category.id,
                    name = category.name,
                    order = category.order.value,
                    channels = channels.map { ChannelUiModel.fromDomain(it) }
                        .map { it.copy(isSelected = it.id.value == selectedChannelId) },
                    isExpanded = expandedCategoryIds.contains(category.id.value)
                )
            }.sortedBy { it.order }

            val directChannelUiModels = data.directChannels.map { channel ->
                ChannelUiModel.fromDomain(channel)
                    .copy(isSelected = channel.id.value == selectedChannelId)
            }
            
            android.util.Log.d("ProjectStructureUiState", "Converting domain data: ${categoryUiModels.size} categories, ${directChannelUiModels.size} direct channels")

            // 통합된 구조 요소 리스트 생성
            val unifiedItems = mutableListOf<ProjectStructureItem>()
            
            // 카테고리들을 통합 리스트에 추가
            categoryUiModels.forEach { category ->
                unifiedItems.add(
                    ProjectStructureItem.CategoryItem(
                        category = category,
                        globalOrder = category.order
                    )
                )
            }
            
            // 직속 채널들을 통합 리스트에 추가 (globalOrder를 0.5로 설정하여 카테고리와 구분)
            directChannelUiModels.forEachIndexed { index, channel ->
                val globalOrder = 0.1 + (index * 0.05) // 0.1, 0.15, 0.2, ... 순서
                android.util.Log.d("ProjectStructureUiState", "Adding direct channel '${channel.name.value}' with order $globalOrder")
                unifiedItems.add(
                    ProjectStructureItem.DirectChannelItem(
                        channel = channel,
                        globalOrder = globalOrder
                    )
                )
            }

            return ProjectStructureUiState(
                categories = categoryUiModels,
                directChannel = directChannelUiModels,
                unifiedStructureItems = unifiedItems.sortedBy { it.globalOrder },
                isLoading = false,
                error = null,
                selectedChannelId = selectedChannelId
            )
        }
        
        /**
         * 로딩 상태를 나타내는 UI 상태를 생성합니다.
         */
        fun loading(): ProjectStructureUiState = ProjectStructureUiState(
            isLoading = true,
            error = null
        )
        
        /**
         * 에러 상태를 나타내는 UI 상태를 생성합니다.
         */
        fun error(errorMessage: String): ProjectStructureUiState = ProjectStructureUiState(
            isLoading = false,
            error = errorMessage
        )
    }
}


/**
 * 카테고리 UI 모델
 * @param id 카테고리 ID
 * @param name 카테고리 이름
 * @param order 카테고리 순서 (정렬용)
 * @param channels 카테고리에 속한 채널 목록
 * @param isExpanded 카테고리 펼침 상태
 */
data class CategoryUiModel(
    val id: DocumentId,
    val name: CategoryName,
    val order: Double,
    val channels: List<ChannelUiModel> = emptyList(),
    val isExpanded: Boolean = true
) {
    companion object {
        fun fromDomain(category: Category, isExpanded: Boolean = true): CategoryUiModel {
            return CategoryUiModel(
                id = category.id,
                name = category.name,
                order = category.order.value,
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
    val id: DocumentId,
    val name: Name,
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

/**
 * 통합된 프로젝트 구조 요소 (카테고리 또는 직속 채널)
 * 카테고리와 직속 채널을 하나의 리스트에서 순서 관리하기 위한 sealed class
 */
sealed interface ProjectStructureItem {
    val id: String
    val globalOrder: Double
    
    data class CategoryItem(
        val category: CategoryUiModel,
        override val globalOrder: Double
    ) : ProjectStructureItem {
        override val id: String = category.id.value
    }
    
    data class DirectChannelItem(
        val channel: ChannelUiModel,
        override val globalOrder: Double
    ) : ProjectStructureItem {
        override val id: String = channel.id.value
    }
}

/**
 * ProjectStructureData를 ProjectStructureUiState로 변환하는 확장 함수
 */
fun ProjectStructureData.toProjectStructureUiState(
    expandedCategoryIds: Set<String> = emptySet(),
    selectedChannelId: String? = null
): ProjectStructureUiState {
    return ProjectStructureUiState.fromDomain(
        data = this,
        expandedCategoryIds = expandedCategoryIds,
        selectedChannelId = selectedChannelId
    )
} 