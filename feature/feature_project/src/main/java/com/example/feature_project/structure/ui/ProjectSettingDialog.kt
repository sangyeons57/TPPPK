package com.example.teamnovapersonalprojectprojectingkotlin.feature_project_structure.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // 필요한 아이콘 임포트
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

// 다이얼로그 내 메뉴 아이템 데이터 클래스 (아이콘, 텍스트, 클릭 액션)
data class ProjectSettingMenuItem(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false, // 삭제 등 위험 작업 여부
    val enabled: Boolean = true // 권한 등에 따른 활성화 여부
)

/**
 * ProjectSettingDialog: 프로젝트 설정을 위한 다양한 메뉴를 제공하는 다이얼로그
 *
 * @param projectName 현재 프로젝트 이름
 * @param onDismissRequest 다이얼로그 닫기 요청
 * @param menuItems 표시할 메뉴 아이템 리스트 (외부에서 정의하여 전달)
 */
@Composable
fun ProjectSettingDialog(
    projectName: String,
    onDismissRequest: () -> Unit,
    menuItems: List<ProjectSettingMenuItem>
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                // 다이얼로그 최대 높이 제한 (내용 많아지면 스크롤)
                .heightIn(max = 600.dp) // 화면 크기에 따라 조절
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 8.dp, start = 8.dp, end = 8.dp) // 내부 패딩 조정
            ) {
                // 프로젝트 이름 (타이틀)
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.headlineMedium, // 제목 크기 조정
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // 설정 메뉴 목록 (스크롤 가능)
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    menuItems.forEachIndexed { index, item ->
                        ProjectSettingDialogButton(
                            text = item.text,
                            icon = item.icon,
                            onClick = {
                                item.onClick()
                                onDismissRequest() // 메뉴 실행 후 다이얼로그 닫기
                            },
                            isDestructive = item.isDestructive,
                            enabled = item.enabled
                        )
                        // 마지막 항목 아니면 구분선 추가 (선택적)
                        if (index < menuItems.lastIndex && !item.isDestructive) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        // 삭제 버튼 위에 간격 추가
                        if (item.isDestructive && index > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // 닫기 버튼 영역
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
 * ProjectSettingDialogButton: 프로젝트 설정 다이얼로그 메뉴 버튼 스타일
 */
@Composable
private fun ProjectSettingDialogButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val contentColor = when {
        isDestructive && enabled -> MaterialTheme.colorScheme.error
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // 비활성화 색상
        else -> LocalContentColor.current
    }

    TextButton( // Button 대신 TextButton 사용 (텍스트 스타일 버튼)
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor) // 텍스트 및 아이콘 색상 지정
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp)) // 아이콘과 텍스트 간격
            Text(text, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f)) // 텍스트 왼쪽 정렬
        }
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun ProjectSettingDialogPreview() {
    // 프리뷰용 메뉴 아이템 정의
    val previewMenuItems = listOf(
        ProjectSettingMenuItem("멤버 초대하기", Icons.Filled.PersonAdd, {}),
        ProjectSettingMenuItem("카테고리 만들기", Icons.Filled.CreateNewFolder, {}),
        // ProjectSettingMenuItem("채널 만들기", Icons.Filled.AddComment, {}), // 채널 만들기는 보통 카테고리 하위에서?
        ProjectSettingMenuItem("프로젝트 편집하기", Icons.Filled.Edit, {}),
        ProjectSettingMenuItem("역할 편집하기", Icons.Filled.ManageAccounts, {}),
        ProjectSettingMenuItem("멤버 편집하기", Icons.Filled.Group, {}),
        ProjectSettingMenuItem("프로젝트 삭제", Icons.Filled.Delete, {}, isDestructive = true)
    )

    TeamnovaPersonalProjectProjectingKotlinTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ProjectSettingDialog(
                projectName = "미리보기 프로젝트",
                onDismissRequest = {},
                menuItems = previewMenuItems
            )
        }
    }
}