package com.example.feature_home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core_common.util.DateTimeUtil
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_home.model.DmUiModel

/**
 * DM 목록 화면
 * 
 * @param dms 표시할 DM 목록
 * @param onDmItemClick DM 아이템 클릭 시 호출될 콜백
 * @param isLoading 로딩 중인지 여부
 * @param modifier Modifier
 */
@Composable
fun DmListComponent(
    modifier: Modifier = Modifier,
    dms: List<DmUiModel>,
    onDmItemClick: (dm: DmUiModel) -> Unit,
    isLoading: Boolean = false,
) {
    if (isLoading && dms.isEmpty()) { // 목록이 비어있고 로딩 중인 경우에만 로더 표시
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (dms.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("활성화된 DM이 없습니다.")
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(dms, key = { it.channelId }) { dmItem ->
            DmListItem(dmItem = dmItem, onClick = { onDmItemClick(dmItem) })
            HorizontalDivider()
        }
    }
}

@Composable
fun DmListItem(
    dmItem: DmUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 프로필 이미지 (또는 이니셜)
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dmItem.partnerName?.firstOrNull()?.toString() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dmItem.partnerName!!,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dmItem.lastMessage ?: "메시지가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = DateTimeUtil.formatDateTime(dmItem.lastMessageTimestamp!!),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
fun DmListItemPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        DmListItem(
            dmItem = DmUiModel(
                channelId = "dm1",
                partnerName = "김철수",
                partnerProfileImageUrl = null,
                lastMessage = "내일 회의 시간에 맞춰서 와주세요. 장소는 동일합니다.",
                lastMessageTimestamp = DateTimeUtil.nowInstant(),
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DmListScreenPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        val sampleDms = listOf(
            DmUiModel(
                channelId = "1", 
                partnerName = "User One", 
                partnerProfileImageUrl = null, 
                lastMessage = "안녕하세요!", 
                lastMessageTimestamp = DateTimeUtil.nowInstant(), 
                unreadCount = 1
            ),
            DmUiModel(
                channelId = "2", 
                partnerName = "User Two With A Very Long Name", 
                partnerProfileImageUrl = null, 
                lastMessage = "긴 메시지 테스트입니다.", 
                lastMessageTimestamp = DateTimeUtil.nowInstant(), 
                unreadCount = 0
            ),
            DmUiModel(
                channelId = "3", 
                partnerName = "User Three", 
                partnerProfileImageUrl = null, 
                lastMessage = null, 
                lastMessageTimestamp = DateTimeUtil.nowInstant(), 
                unreadCount = 2
            )
        )
        DmListComponent(dms = sampleDms, onDmItemClick = {}, isLoading = false)
    }
}

@Preview(showBackground = true)
@Composable
fun DmListScreenEmptyPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        DmListComponent(dms = emptyList(), onDmItemClick = {}, isLoading = false)
    }
}

@Preview(showBackground = true)
@Composable
fun DmListScreenLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        DmListComponent(dms = emptyList(), onDmItemClick = {}, isLoading = true)
    }
} 