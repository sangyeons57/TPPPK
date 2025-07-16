package com.example.feature_home.ui.component

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.core_ui.components.reorder.SimpleReorderDialog
import com.example.feature_edit_category.ui.EditCategoryDialog
import com.example.feature_edit_channel.ui.EditChannelDialog
import com.example.feature_home.dialog.ui.AddProjectElementDialog
import com.example.feature_home.model.ProjectStructureUiState
import com.example.feature_home.model.toUnifiedDialogItems
import com.example.feature_home.model.toProjectStructureItems
import com.example.feature_home.viewmodel.HomeViewModel

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


    // ReorderUnifiedProjectStructureDialog
    if (dialogStates.showReorderProjectStructureDialog && uiState.selectedProjectId != null) {
        ReorderUnifiedProjectStructureDialog(
            projectStructure = uiState.projectStructure,
            onDismiss = { 
                onDialogStateChange(dialogStates.copy(showReorderProjectStructureDialog = false))
            },
            onReorderComplete = { reorderedItems ->
                viewModel.onReorderUnifiedProjectStructure(uiState.selectedProjectId!!, reorderedItems)
                onDialogStateChange(dialogStates.copy(showReorderProjectStructureDialog = false))
            }
        )
    }
}


/**
 * 통합 프로젝트 구조 순서 변경 다이얼로그
 */
@Composable
private fun ReorderUnifiedProjectStructureDialog(
    projectStructure: ProjectStructureUiState,
    onDismiss: () -> Unit,
    onReorderComplete: (List<com.example.feature_home.model.ProjectStructureItem>) -> Unit
) {
    val dialogItems = projectStructure.toUnifiedDialogItems()

    SimpleReorderDialog(
        title = "프로젝트 구조 순서 변경",
        items = dialogItems,
        itemKey = { it.id },
        itemLabel = { it.displayName },
        onDismiss = onDismiss,
        onReorderComplete = { reorderedDialogItems ->
            val reorderedProjectStructureItems = reorderedDialogItems.toProjectStructureItems()
            onReorderComplete(reorderedProjectStructureItems)
        }
    )
}