package com.example.feature_home.dialog.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialog
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialogBuilder
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

/**
 * AddProjectElementDialog: 프로젝트 요소 추가 옵션을 제공하는 바텀시트 다이얼로그 Composable
 * 기존 탭 기반 다이얼로그를 바텀시트 형태로 변경
 *
 * @param onDismissRequest 다이얼로그 닫기 요청 콜백
 * @param onAddCategoryClick '카테고리 추가하기' 클릭 콜백
 * @param onAddChannelClick '채널 추가하기' 클릭 콜백
 */
@Composable
fun AddProjectElementDialog(
    onDismissRequest: () -> Unit,
    onAddCategoryClick: () -> Unit,
    onAddChannelClick: () -> Unit,
) {
    val bottomSheetItems = remember {
        BottomSheetDialogBuilder()
            .text("프로젝트 구조 편집")
            .spacer(height = 8.dp)
            .button(
                label = "카테고리 추가하기",
                icon = Icons.Filled.Add,
                onClick = {
                    onAddCategoryClick()
                    onDismissRequest()
                }
            )
            .button(
                label = "채널 추가하기",
                icon = Icons.Filled.Add,
                onClick = {
                    onAddChannelClick()
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
private fun AddProjectElementDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AddProjectElementDialog(
            onDismissRequest = {},
            onAddCategoryClick = {},
            onAddChannelClick = {}
        )
    }
}