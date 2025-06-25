package com.example.feature_edit_category.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

/**
 * EditCategoryDialog: 카테고리 편집 및 채널 추가 옵션을 제공하는 다이얼로그 Composable
 *
 * @param categoryName 현재 선택된 카테고리 이름
 * @param onDismissRequest 다이얼로그 닫기 요청 콜백
 * @param onEditCategoryClick '카테고리 편집하기' 클릭 콜백
 * @param onCreateChannelClick '채널 추가하기' 클릭 콜백
 */
@Composable
fun EditCategoryDialog(
    categoryName: String,
    onDismissRequest: () -> Unit,
    onEditCategoryClick: () -> Unit,
    onCreateChannelClick: () -> Unit,
    // TODO: 권한에 따른 버튼 활성화/비활성화 로직 추가 필요 (예: canEditStructure: Boolean)
) {
    // Dialog Composable 사용
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .padding(24.dp) // 다이얼로그 내부 패딩
            ) {
                // 카테고리 이름 표시 (타이틀 역할)
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center, // 중앙 정렬
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 메뉴 버튼들
                DialogMenuButton(
                    text = "카테고리 편집하기",
                    icon = Icons.Filled.Edit,
                    onClick = {
                        onEditCategoryClick()
                        onDismissRequest() // 동작 후 다이얼로그 닫기
                    }
                )

                DialogMenuButton(
                    text = "채널 추가하기",
                    icon = Icons.Filled.Add,
                    onClick = {
                        onCreateChannelClick()
                        onDismissRequest() // 동작 후 다이얼로그 닫기
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 닫기 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("닫기")
                    }
                }
            }
        }
    }
}

/**
 * 다이얼로그 내 메뉴 버튼 스타일
 */
@Composable
private fun DialogMenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true // 권한 등에 따른 활성화 여부
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp) // 버튼 내부 패딩
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth() // 내부 요소 정렬 위해
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text)
            Spacer(modifier = Modifier.weight(1f)) // 텍스트를 왼쪽으로 밀착
        }
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun EditCategoryDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        // 다이얼로그 프리뷰를 위해 배경에 Box 추가
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EditCategoryDialog(
                categoryName = "일반",
                onDismissRequest = {},
                onEditCategoryClick = {},
                onCreateChannelClick = {}
            )
        }
    }
}