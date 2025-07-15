package com.example.feature_home.ui.component

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.core_ui.components.reorder.SimpleReorderDialog
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.feature_edit_category.ui.EditCategoryDialog
import com.example.feature_edit_channel.ui.EditChannelDialog
import com.example.feature_home.dialog.ui.AddProjectElementDialog
import com.example.feature_home.model.ProjectStructureUiState
import com.example.feature_home.viewmodel.HomeViewModel
import java.time.Instant

/**
 * HomeScreen의 모든 다이얼로그를 관리하는 컴포넌트
 */
@Composable
fun HomeScreenDialogs(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    uiState: com.example.feature_home.viewmodel.HomeUiState,
    dialogStates: DialogStates,
    onDialogStateChange: (DialogStates) -> Unit
) {
    // AddProjectElementDialog
    if (dialogStates.showAddProjectElementDialog && dialogStates.currentProjectIdForDialog != null) {
        AddProjectElementDialog(
            projectId = dialogStates.currentProjectIdForDialog?.value ?: "",
            onDismissRequest = {
                onDialogStateChange(
                    dialogStates.copy(
                        showAddProjectElementDialog = false,
                        currentProjectIdForDialog = null
                    )
                )
                // 다이얼로그가 닫힐 때 프로젝트 구조를 새로고침합니다.
                uiState.selectedProjectId?.let { viewModel.refreshProjectStructure(it) }
            },
            onCategoryCreated = { category ->
                // 카테고리 생성 후 프로젝트 구조 새로고침
                uiState.selectedProjectId?.let { viewModel.refreshProjectStructure(it) }
            },
            onChannelCreated = { channel ->
                // 채널 생성 후 프로젝트 구조 새로고침
                uiState.selectedProjectId?.let { viewModel.refreshProjectStructure(it) }
            }
        )
    }

    // EditCategoryDialog
    if (dialogStates.showEditCategoryDialog) {
        EditCategoryDialog(
            categoryName = dialogStates.editCategoryName,
            projectId = dialogStates.editCategoryProjectId,
            categoryId = dialogStates.editCategoryId,
            onDismissRequest = { 
                onDialogStateChange(dialogStates.copy(showEditCategoryDialog = false))
            },
            onNavigateToEditCategory = {
                // Dialog는 자동으로 닫히므로 별도 처리 불필요
                // EditCategoryDialogViewModel에서 navigation 처리됨
            },
            onShowReorderDialog = {
                onDialogStateChange(
                    dialogStates.copy(
                        showEditCategoryDialog = false,
                        showReorderCategoriesDialog = true
                    )
                )
            }
        )
    }

    // EditChannelDialog
    if (dialogStates.showEditChannelDialog) {
        EditChannelDialog(
            channelName = dialogStates.editChannelName,
            projectId = dialogStates.editChannelProjectId,
            channelId = dialogStates.editChannelId,
            onDismissRequest = { 
                onDialogStateChange(dialogStates.copy(showEditChannelDialog = false))
            },
            onNavigateToEditChannel = {
                onDialogStateChange(dialogStates.copy(showEditChannelDialog = false))
            }
        )
    }

    // ReorderCategoriesDialog
    if (dialogStates.showReorderCategoriesDialog && uiState.selectedProjectId != null) {
        ReorderCategoriesDialog(
            projectStructure = uiState.projectStructure,
            onDismiss = { 
                onDialogStateChange(dialogStates.copy(showReorderCategoriesDialog = false))
            },
            onReorderComplete = { reorderedCategories ->
                viewModel.onReorderCategories(uiState.selectedProjectId!!, reorderedCategories)
                onDialogStateChange(dialogStates.copy(showReorderCategoriesDialog = false))
            }
        )
    }

    // ReorderChannelsDialog
    if (dialogStates.showReorderChannelsDialog && uiState.selectedProjectId != null) {
        ReorderChannelsDialog(
            projectStructure = uiState.projectStructure,
            reorderCategoryId = dialogStates.reorderCategoryId,
            onDismiss = { 
                onDialogStateChange(
                    dialogStates.copy(
                        showReorderChannelsDialog = false,
                        reorderCategoryId = null
                    )
                )
            },
            onReorderComplete = { reorderedChannels ->
                val categoryDocumentId = dialogStates.reorderCategoryId?.let { DocumentId(it) }
                viewModel.onReorderChannels(uiState.selectedProjectId!!, categoryDocumentId, reorderedChannels)
                onDialogStateChange(
                    dialogStates.copy(
                        showReorderChannelsDialog = false,
                        reorderCategoryId = null
                    )
                )
            }
        )
    }
}

/**
 * 카테고리 순서 변경 다이얼로그
 */
@Composable
private fun ReorderCategoriesDialog(
    projectStructure: ProjectStructureUiState,
    onDismiss: () -> Unit,
    onReorderComplete: (List<com.example.domain.model.base.Category>) -> Unit
) {
    val categories = projectStructure.categories
        .filter { it.id.value != com.example.domain.model.base.Category.NO_CATEGORY_ID }
        .map { categoryUiModel ->
            // CategoryUiModel을 Category 도메인 객체로 변환
            com.example.domain.model.base.Category.fromDataSource(
                id = categoryUiModel.id,
                name = categoryUiModel.name,
                order = com.example.domain.model.vo.category.CategoryOrder(categoryUiModel.order),
                createdBy = OwnerId("system"),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                isCategory = com.example.domain.model.vo.category.IsCategoryFlag.TRUE
            )
        }

    SimpleReorderDialog(
        title = "카테고리 순서 변경",
        items = categories,
        itemKey = { it.id.value },
        itemLabel = { it.name.value },
        onDismiss = onDismiss,
        onReorderComplete = onReorderComplete
    )
}

/**
 * 채널 순서 변경 다이얼로그
 */
@Composable
private fun ReorderChannelsDialog(
    projectStructure: ProjectStructureUiState,
    reorderCategoryId: String?,
    onDismiss: () -> Unit,
    onReorderComplete: (List<ProjectChannel>) -> Unit
) {
    val channels = if (reorderCategoryId == null) {
        // 프로젝트 직속 채널들
        projectStructure.directChannel
    } else {
        // 특정 카테고리의 채널들
        projectStructure.categories
            .find { it.id.value == reorderCategoryId }
            ?.channels ?: emptyList()
    }.map { channelUiModel ->
        // ChannelUiModel을 ProjectChannel 도메인 객체로 변환
        ProjectChannel.create(
            channelName = channelUiModel.name,
            channelType = channelUiModel.mode,
            order = ProjectChannelOrder(0.0), // 임시 order, 실제로는 reorder use case에서 재설정됨
            categoryId = DocumentId(reorderCategoryId ?: com.example.domain.model.base.Category.NO_CATEGORY_ID)
        )
    }

    val categoryName = if (reorderCategoryId == null) {
        "프로젝트 직속 채널"
    } else {
        projectStructure.categories
            .find { it.id.value == reorderCategoryId }
            ?.name?.value ?: "채널"
    }

    SimpleReorderDialog(
        title = "$categoryName 순서 변경",
        items = channels,
        itemKey = { it.id.value },
        itemLabel = { it.channelName.value },
        onDismiss = onDismiss,
        onReorderComplete = onReorderComplete
    )
}