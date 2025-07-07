package com.example.feature_edit_channel.dialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialog
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialogBuilder
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

/**
 * EditChannelDialog: 채널 편집 옵션을 제공하는 바텀시트 다이얼로그 Composable
 *
 * @param channelName 현재 선택된 채널 이름
 * @param onDismissRequest 다이얼로그 닫기 요청 콜백
 * @param onEditChannelClick '채널 편집하기' 클릭 콜백
 */
@Composable
fun EditChannelDialog(
    channelName: String,
    onDismissRequest: () -> Unit,
    onEditChannelClick: () -> Unit,
) {
    val bottomSheetItems = remember(channelName) {
        BottomSheetDialogBuilder()
            .text(channelName) // 채널 이름을 타이틀로 표시
            .spacer(height = 8.dp)
            .button(
                label = "채널 편집하기",
                icon = Icons.Filled.Edit,
                onClick = {
                    onEditChannelClick()
                    onDismissRequest()
                }
            )
            .spacer(height = 16.dp)
            .build()
    }

    BottomSheetDialog(
        items = bottomSheetItems,
        onDismiss = onDismissRequest
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
            onEditChannelClick = {}
        )
    }
}