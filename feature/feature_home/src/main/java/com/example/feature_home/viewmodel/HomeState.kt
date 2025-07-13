package com.example.feature_home.viewmodel

import com.example.domain.model.vo.DocumentId
import com.example.feature_home.model.CategoryUiModel
import com.example.feature_home.model.ChannelUiModel
import com.example.feature_home.model.DmUiModel
import com.example.feature_home.model.ProjectStructureUiState
import com.example.feature_home.model.ProjectUiModel
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialogItem

/** 홈 화면 상단 섹션 */
enum class TopSection {
    PROJECTS, DMS
}

/** 프로젝트 멤버 정보 */
data class ProjectMember(
    val id: String,
    val name: String,
    val role: String,
)

/** 홈 화면 UI 상태 */
data class HomeUiState(
    val selectedTopSection: TopSection = TopSection.DMS,
    val projects: List<ProjectUiModel> = emptyList(),
    val dms: List<DmUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String = "default",
    val selectedProjectId: DocumentId? = null,
    val selectedDmId: String? = null,
    val projectName: String = "",
    val projectDescription: String? = null,
    val projectMembers: List<ProjectMember> = emptyList(),
    val projectStructure: ProjectStructureUiState = ProjectStructureUiState(),
    val userInitial: String = "U",
    val userProfileImageUrl: String? = null,
    val isDetailFullScreen: Boolean = false,
    val showBottomSheet: Boolean = false,
    val showBottomSheetItems: List<BottomSheetDialogItem> = emptyList(),
    val targetDMChannelForSheet: DmUiModel? = null,
    val targetCategoryForSheet: CategoryUiModel? = null,
    val targetChannelForSheet: ChannelUiModel? = null,
)
