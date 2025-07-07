package com.example.feature_project_detail.structure.ui // 또는 공통 ui 패키지

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import java.time.Instant

/**
 * ChannelListItem: 프로젝트 내 채널 목록의 아이템 UI
 * (item_channel.xml 변환 결과)
 *
 * @param channel 표시할 채널 정보 (Using domain model Channel)
 * @param onClick 채널 아이템 클릭 시 실행될 콜백 (채팅방 이동 등)
 * @param modifier Modifier
 * @param showActions 수정/삭제 등 추가 액션 버튼 표시 여부 (옵션)
 * @param onEditClick 수정 버튼 클릭 콜백 (showActions = true 일 때)
 * @param onDeleteClick 삭제 버튼 클릭 콜백 (showActions = true 일 때)
 */
@Composable
fun ChannelListItem(
    channel: ProjectChannel,
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
        val icon: ImageVector = when (channel.channelType) {
            ProjectChannelType.MESSAGES -> Icons.Filled.ChatBubbleOutline
            else -> Icons.Filled.ChatBubbleOutline // Default icon or handle error
        }
        Icon(
            imageVector = icon,
            contentDescription = "${channel.channelType} 채널",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.secondary // 아이콘 색상
        )

        Spacer(modifier = Modifier.width(12.dp)) // 아이콘과 텍스트 간격

        // 채널 이름
        Text(
            text = channel.channelName.value,
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
    val textChannel = ProjectChannel.fromDataSource(
        id = DocumentId.from("ch1"),
        channelName = Name("일반 대화"),
        order = ProjectChannelOrder.from(1),
        channelType = ProjectChannelType.MESSAGES,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            ChannelListItem(channel = textChannel, onClick = {})
        }
    }
}