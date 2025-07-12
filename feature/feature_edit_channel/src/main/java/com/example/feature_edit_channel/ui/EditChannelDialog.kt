package com.example.feature_edit_channel.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialog
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialogBuilder
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_edit_channel.viewmodel.EditChannelDialogEvent
import com.example.feature_edit_channel.viewmodel.EditChannelDialogViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * EditChannelDialog: 채널 편집 옵션을 제공하는 바텀시트 다이얼로그 Composable
 * HiltViewModel을 사용하여 비즈니스 로직을 처리합니다.
 *
 * @param channelName 현재 선택된 채널 이름
 * @param onDismissRequest 다이얼로그 닫기 요청 콜백
 * @param onNavigateToEditChannel 채널 편집 화면으로 이동 콜백
 * @param viewModel EditChannelDialogViewModel 인스턴스
 */
@Composable
fun EditChannelDialog(
    channelName: String,
    onDismissRequest: () -> Unit,
    onNavigateToEditChannel: () -> Unit = {},
    viewModel: EditChannelDialogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 채널 이름 초기화
    LaunchedEffect(channelName) {
        viewModel.initialize(channelName)
    }

    // 이벤트 처리
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditChannelDialogEvent.DismissDialog -> {
                    onDismissRequest()
                }
                is EditChannelDialogEvent.NavigateToEditChannel -> {
                    onNavigateToEditChannel()
                }
            }
        }
    }

    val bottomSheetItems = remember(uiState.channelName) {
        BottomSheetDialogBuilder()
            .text(uiState.channelName) // 채널 이름을 타이틀로 표시
            .spacer(height = 8.dp)
            .button(
                label = "채널 편집하기",
                icon = Icons.Filled.Edit,
                onClick = viewModel::onEditChannelClick
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
private fun EditChannelDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditChannelDialog(
            channelName = "일반 대화",
            onDismissRequest = {},
            onNavigateToEditChannel = {}
        )
    }
}