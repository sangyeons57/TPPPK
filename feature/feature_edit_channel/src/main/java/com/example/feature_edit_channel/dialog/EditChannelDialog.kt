package com.example.feature_edit_channel.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

/**
 * EditChannelDialog: 채널 편집 옵션을 제공하는 다이얼로그 Composable
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
    // TODO: 권한에 따른 버튼 활성화/비활성화 로직 추가 필요
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
            ) {
                // 채널 이름 표시 (타이틀 역할)
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 채널 편집 버튼
                DialogMenuButton( // 이전 EditCategoryDialog에서 사용한 버튼 재활용
                    text = "채널 편집하기",
                    icon = Icons.Filled.Edit,
                    onClick = {
                        onEditChannelClick()
                        onDismissRequest() // 동작 후 다이얼로그 닫기
                    }
                )

                // TODO: 채널 관련 다른 액션 버튼 추가 가능 (예: 채널 복제, 이동, 권한 설정 등)

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
 * 다이얼로그 내 메뉴 버튼 스타일 (EditCategoryDialog와 공유 가능, 별도 파일로 분리 권장)
 */
@Composable
private fun DialogMenuButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun EditChannelDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EditChannelDialog(
                channelName = "일반 대화",
                onDismissRequest = {},
                onEditChannelClick = {}
            )
        }
    }
}