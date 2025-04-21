package com.example.teamnovapersonalprojectprojectingkotlin.feature_project_structure.ui // 또는 공통 ui 패키지

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline // 텍스트 채널 아이콘
import androidx.compose.material.icons.filled.Delete // 삭제 아이콘 (옵션)
import androidx.compose.material.icons.filled.Edit // 편집 아이콘 (옵션)
import androidx.compose.material.icons.filled.VolumeUp // 음성 채널 아이콘
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project_structure.viewmodel.ChannelType // 실제 ChannelType enum 경로
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

// 채널 정보를 담는 데이터 클래스 (호출하는 쪽에서 사용)
// 예시: feature_project_setting/viewmodel/ProjectSettingViewModel.kt 에 정의된 ProjectChannel 사용 가능
data class ChannelInfo(
    val id: String,
    val name: String,
    val type: ChannelType // TEXT 또는 VOICE
)

/**
 * ChannelListItem: 프로젝트 내 채널 목록의 아이템 UI
 * (item_channel.xml 변환 결과)
 *
 * @param channel 표시할 채널 정보
 * @param onClick 채널 아이템 클릭 시 실행될 콜백 (채팅방 이동 등)
 * @param modifier Modifier
 * @param showActions 수정/삭제 등 추가 액션 버튼 표시 여부 (옵션)
 * @param onEditClick 수정 버튼 클릭 콜백 (showActions = true 일 때)
 * @param onDeleteClick 삭제 버튼 클릭 콜백 (showActions = true 일 때)
 */
@Composable
fun ChannelListItem(
    channel: ChannelInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showActions: Boolean = false, // 추가 액션 버튼 표시 여부
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = if (showActions) 32.dp else 16.dp, // 액션 없을 시 들여쓰기 줄임
                end = if (showActions) 4.dp else 16.dp, // 액션 없을 시 오른쪽 패딩 늘림
                top = 12.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 채널 타입 아이콘
        val icon: ImageVector = when (channel.type) {
            ChannelType.TEXT -> Icons.Filled.ChatBubbleOutline // 텍스트 채널 아이콘
            ChannelType.VOICE -> Icons.Filled.VolumeUp // 음성 채널 아이콘
        }
        Icon(
            imageVector = icon,
            contentDescription = "${channel.type} 채널",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.secondary // 아이콘 색상
        )

        Spacer(modifier = Modifier.width(12.dp)) // 아이콘과 텍스트 간격

        // 채널 이름
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f) // 남은 공간 차지
        )

        // 추가 액션 버튼 (편집/삭제 등) - showActions가 true일 때만 표시
        if (showActions) {
            Row {
                onEditClick?.let { // 수정 콜백이 있으면 버튼 표시
                    IconButton(onClick = it, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "채널 편집", modifier = Modifier.size(20.dp))
                    }
                }
                onDeleteClick?.let { // 삭제 콜백이 있으면 버튼 표시
                    IconButton(onClick = it, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "채널 삭제", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// --- Preview ---
@Preview(showBackground = true, name = "Text Channel Item")
@Composable
private fun ChannelListItemTextPreview() {
    val channel = ChannelInfo("ch1", "일반 대화", ChannelType.TEXT)
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            ChannelListItem(channel = channel, onClick = {})
        }
    }
}

@Preview(showBackground = true, name = "Voice Channel Item")
@Composable
private fun ChannelListItemVoicePreview() {
    val channel = ChannelInfo("ch2", "음성 회의", ChannelType.VOICE)
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            ChannelListItem(channel = channel, onClick = {})
        }
    }
}

@Preview(showBackground = true, name = "Channel Item with Actions")
@Composable
private fun ChannelListItemWithActionsCallback() {
    val channel = ChannelInfo("ch3", "공지사항", ChannelType.TEXT)
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            ChannelListItem(
                channel = channel,
                onClick = {},
                showActions = true, // 액션 버튼 표시
                onEditClick = {},
                onDeleteClick = {}
            )
        }
    }
}