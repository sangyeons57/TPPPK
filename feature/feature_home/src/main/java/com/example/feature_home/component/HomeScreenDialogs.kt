package com.example.feature_home.ui.component

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.core_ui.components.reorder.SimpleReorderDialog
import com.example.feature_home.dialog.ui.AddProjectElementDialog
import com.example.feature_home.model.ChannelUiModel
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
    
    // ReorderCategoryChannelsDialog
    if (dialogStates.showReorderCategoryChannelsDialog && 
        dialogStates.currentCategoryIdForDialog != null && 
        uiState.selectedProjectId != null) {
        ReorderCategoryChannelsDialog(
            projectStructure = uiState.projectStructure,
            categoryId = dialogStates.currentCategoryIdForDialog,
            onDismiss = { 
                onDialogStateChange(dialogStates.copy(
                    showReorderCategoryChannelsDialog = false,
                    currentCategoryIdForDialog = null
                ))
            },
            onReorderComplete = { reorderedChannels ->
                viewModel.onReorderCategoryChannels(
                    projectId = uiState.selectedProjectId!!,
                    categoryId = dialogStates.currentCategoryIdForDialog!!,
                    reorderedChannels = reorderedChannels
                )
                onDialogStateChange(dialogStates.copy(
                    showReorderCategoryChannelsDialog = false,
                    currentCategoryIdForDialog = null
                ))
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

/**
 * 카테고리 내 채널 순서 변경 다이얼로그
 */
@Composable
private fun ReorderCategoryChannelsDialog(
    projectStructure: ProjectStructureUiState,
    categoryId: com.example.domain.model.vo.DocumentId,
    onDismiss: () -> Unit,
    onReorderComplete: (List<ChannelUiModel>) -> Unit
) {
    // 해당 카테고리의 채널 목록 가져오기
    val categoryChannels = projectStructure.categories
        .find { it.id == categoryId }
        ?.channels
        ?: emptyList()
    
    val categoryName = projectStructure.categories
        .find { it.id == categoryId }
        ?.name?.value
        ?: "카테고리"

    SimpleReorderDialog(
        title = "$categoryName 채널 순서 변경",
        items = categoryChannels,
        itemKey = { it.id.value },
        itemLabel = { channel ->
            val channelIcon = when (channel.mode) {
                com.example.domain.model.enum.ProjectChannelType.MESSAGES -> "#"
                com.example.domain.model.enum.ProjectChannelType.TASKS -> "◉"
                else -> "#"
            }
            "$channelIcon ${channel.name.value}"
        },
        onDismiss = onDismiss,
        onReorderComplete = { reorderedChannels ->
            onReorderComplete(reorderedChannels)
        }
    )
}

