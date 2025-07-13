package com.example.feature_edit_category.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialog
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialogBuilder
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_edit_category.viewmodel.EditCategoryDialogEvent
import com.example.feature_edit_category.viewmodel.EditCategoryDialogViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * EditCategoryDialog: 카테고리 편집 및 채널 추가 옵션을 제공하는 바텀시트 다이얼로그 Composable
 * HiltViewModel을 사용하여 비즈니스 로직을 처리합니다.
 *
 * @param categoryName 현재 선택된 카테고리 이름
 * @param projectId 프로젝트 ID
 * @param categoryId 카테고리 ID
 * @param onDismissRequest 다이얼로그 닫기 요청 콜백
 * @param onNavigateToEditCategory 카테고리 편집 화면으로 이동 콜백
 * @param onNavigateToCreateChannel 채널 생성 화면으로 이동 콜백
 * @param viewModel EditCategoryDialogViewModel 인스턴스
 */
@Composable
fun EditCategoryDialog(
    categoryName: String,
    projectId: String,
    categoryId: String,
    onDismissRequest: () -> Unit,
    onNavigateToEditCategory: () -> Unit = {},
    onNavigateToCreateChannel: () -> Unit = {},
    viewModel: EditCategoryDialogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 카테고리 이름 초기화
    LaunchedEffect(categoryName, projectId, categoryId) {
        viewModel.initialize(categoryName, projectId, categoryId)
    }

    // 이벤트 처리
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditCategoryDialogEvent.DismissDialog -> {
                    onDismissRequest()
                }
                is EditCategoryDialogEvent.NavigateToEditCategory -> {
                    onNavigateToEditCategory()
                }
                is EditCategoryDialogEvent.NavigateToCreateChannel -> {
                    onNavigateToCreateChannel()
                }
            }
        }
    }

    val bottomSheetItems = remember(uiState.categoryName) {
        BottomSheetDialogBuilder()
            .text(uiState.categoryName) // 카테고리 이름을 타이틀로 표시
            .spacer(height = 8.dp)
            .button(
                label = "카테고리 편집하기",
                icon = Icons.Filled.Edit,
                onClick = viewModel::onEditCategoryClick
            )
            .button(
                label = "채널 추가하기",
                icon = Icons.Filled.Add,
                onClick = viewModel::onCreateChannelClick
            )
            .spacer(height = 16.dp)
            .build()
    }

    BottomSheetDialog(
        items = bottomSheetItems,
        onDismiss = viewModel::onDismiss
    )
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun EditCategoryDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditCategoryDialog(
            categoryName = "일반",
            projectId = "project123",
            categoryId = "category123",
            onDismissRequest = {},
            onNavigateToEditCategory = {},
            onNavigateToCreateChannel = {}
        )
    }
}