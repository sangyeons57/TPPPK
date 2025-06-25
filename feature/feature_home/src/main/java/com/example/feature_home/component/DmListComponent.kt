package com.example.feature_home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserName
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
        items(dms, key = { it.channelId.value }) { dmItem ->
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
                text = dmItem.partnerName?.value?.firstOrNull()?.toString() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dmItem.partnerName?.value ?: "Unknown User",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
    }
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
fun DmListItemPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        DmListItem(
            dmItem = DmUiModel(
                channelId = DocumentId("dm1"),
                partnerName = UserName("김철수"),
                partnerProfileImageUrl = null,
                unreadCount = 1
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
                channelId = DocumentId("1"),
                partnerName = UserName("User One"), 
                partnerProfileImageUrl = null, 
                unreadCount = 1
            ),
            DmUiModel(
                channelId = DocumentId("2"),
                partnerName = UserName("User Two With A Very Long Name"), 
                partnerProfileImageUrl = null, 
                unreadCount = 0
            ),
            DmUiModel(
                channelId = DocumentId("3"),
                partnerName = UserName("User Three"), 
                partnerProfileImageUrl = null, 
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