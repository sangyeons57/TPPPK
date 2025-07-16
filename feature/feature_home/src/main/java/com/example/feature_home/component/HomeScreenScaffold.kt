package com.example.feature_home.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialog
import com.example.feature_home.component.MainHomeFloatingButton
import com.example.feature_home.viewmodel.HomeUiState
import com.example.feature_home.viewmodel.HomeViewModel
import com.example.feature_home.viewmodel.TopSection

// 오버레이 투명도 상수
private const val OVERLAY_ALPHA = 0.7f

/**
 * HomeScreen의 Scaffold 구성을 담당하는 컴포넌트
 */
@Composable
fun HomeScreenScaffold(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    dialogStates: DialogStates,
    onDialogStateChange: (DialogStates) -> Unit,
    content: @Composable () -> Unit
) {
    // Bottom Sheet 다이얼로그
    if (uiState.showBottomSheet) {
        BottomSheetDialog(
            items = uiState.showBottomSheetItems,
            onDismiss = viewModel::onProjectItemActionSheetDismiss
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            MainHomeFloatingButton(
                currentSection = uiState.selectedTopSection,
                isExpanded = dialogStates.showFloatingMenu,
                onExpandedChange = { expanded ->
                    onDialogStateChange(dialogStates.copy(showFloatingMenu = expanded))
                },
                onAddProject = viewModel::onProjectAddButtonClick,
                onAddDm = viewModel::onAddFriendClick,
                onAddProjectElement = { 
                    uiState.selectedProjectId?.let { viewModel.onAddProjectElement(it) }
                }
            )
        }
    ) { paddingValues ->
        // 메인 콘텐츠와 오버레이를 포함하는 Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 메인 콘텐츠
            content()

            // 배경 오버레이 (다이얼로그 또는 메뉴가 열렸을 때 표시)
            AnimatedVisibility(
                visible = dialogStates.showFloatingMenu,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = OVERLAY_ALPHA))
                        .clickable {
                            if (dialogStates.showFloatingMenu) {
                                onDialogStateChange(dialogStates.copy(showFloatingMenu = false))
                            }
                        }
                )
            }

            // 다이얼로그들 (zIndex를 높게 설정하여 오버레이 위에 표시)
            HomeScreenDialogs(
                modifier = Modifier.zIndex(2f),
                viewModel = viewModel,
                uiState = uiState,
                dialogStates = dialogStates,
                onDialogStateChange = onDialogStateChange
            )
        }
    }
}