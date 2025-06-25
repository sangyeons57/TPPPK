package com.example.feature_project.structure.ui // 또는 공통 ui 패키지

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import java.time.Instant

// 카테고리 및 채널 정보를 담는 데이터 클래스 (호출하는 쪽에서 사용)
// 예시: feature_project_setting/viewmodel/ProjectSettingViewModel.kt 에 정의된 모델 사용 가능
data class ExpandableCategoryData(
    val id: String,
    val name: String,
    val channels: List<ProjectChannel> = emptyList(), // Changed from ChannelInfo to domain.model.Channel
    val isExpanded: Boolean = false // 확장 상태
)

/**
 * ExpandableCategoryItem: 확장 가능한 카테고리 및 하위 채널 목록 UI
 * (item_channellist.xml 변환 결과)
 *
 * @param category 표시할 카테고리 정보 (확장 상태 포함)
 * @param onCategoryClick 카테고리 헤더 클릭 시 호출될 콜백 (확장/축소 처리)
 * @param onChannelClick 채널 아이템 클릭 시 호출될 콜백 (채팅방 이동 등)
 * @param modifier Modifier
 * @param showActions 카테고리/채널 편집/삭제 등 추가 액션 버튼 표시 여부 (옵션)
 * @param onCategoryEditClick 카테고리 편집 버튼 클릭 콜백
 * @param onCategoryDeleteClick 카테고리 삭제 버튼 클릭 콜백
 * @param onAddChannelClick 카테고리 내 채널 추가 버튼 클릭 콜백
 * @param onChannelEditClick 채널 편집 버튼 클릭 콜백
 * @param onChannelDeleteClick 채널 삭제 버튼 클릭 콜백
 */
@Composable
fun ExpandableCategoryItem(
    category: ExpandableCategoryData,
    onCategoryClick: () -> Unit,
    onChannelClick: (ProjectChannel) -> Unit,
    modifier: Modifier = Modifier,
    showActions: Boolean = false,
    onCategoryEditClick: (() -> Unit)? = null,
    onCategoryDeleteClick: (() -> Unit)? = null,
    onAddChannelClick: (() -> Unit)? = null,
    onChannelEditClick: ((ProjectChannel) -> Unit)? = null,
    onChannelDeleteClick: ((ProjectChannel) -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 카테고리 헤더 (클릭 시 확장/축소)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCategoryClick) // 카테고리 클릭 시 확장/축소
                .background(
                    if (showActions) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    else Color.Transparent // 편집 모드일 때만 배경색 살짝
                )
                .padding(
                    start = 16.dp,
                    end = if (showActions) 4.dp else 16.dp, // 액션 있을 때 오른쪽 패딩 줄임
                    top = 16.dp, // 패딩 조정
                    bottom = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 확장/축소 아이콘
            Icon(
                imageVector = if (category.isExpanded) Icons.Filled.ArrowDropDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (category.isExpanded) "축소" else "확장",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // 편집 모드일 때 액션 버튼들
            if (showActions) {
                Row {
                    onCategoryEditClick?.let {
                        IconButton(onClick = it, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = "카테고리 편집", modifier = Modifier.size(20.dp))
                        }
                    }
                    onAddChannelClick?.let {
                        IconButton(onClick = it, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Add, contentDescription = "채널 추가", modifier = Modifier.size(20.dp))
                        }
                    }
                    onCategoryDeleteClick?.let {
                        IconButton(onClick = it, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "카테고리 삭제", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // 확장된 채널 목록
        AnimatedVisibility(visible = category.isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp) // 카테고리보다 들여쓰기
            ) {
                category.channels.forEach { channel ->
                    ChannelListItem( // 이전에 만든 ChannelListItem 재사용
                        channel = channel,
                        onClick = { onChannelClick(channel) },
                        showActions = showActions, // 편집 모드 상속
                        onEditClick = { onChannelEditClick?.invoke(channel) },
                        onDeleteClick = { onChannelDeleteClick?.invoke(channel) }
                    )
                    if (!showActions) { // 편집 모드가 아닐 때만 구분선 표시
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}


// --- Preview ---
@Preview(showBackground = true, name = "Collapsed Category")
@Composable
private fun ExpandableCategoryItemCollapsedPreview() {
    val category = ExpandableCategoryData(
        id = "c1", name = "일반", channels = listOf(
            ProjectChannel.fromDataSource(
                id = DocumentId.from("ch1"),
                channelName = Name("일반"),
                order = ProjectChannelOrder.from(1),
                channelType = ProjectChannelType.MESSAGES,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        ), isExpanded = false
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            ExpandableCategoryItem(category = category, onCategoryClick = {}, onChannelClick = {})
        }
    }
}

@Preview(showBackground = true, name = "Expanded Category")
@Composable
private fun ExpandableCategoryItemExpandedPreview() {
    val category = ExpandableCategoryData(
        id = "c2", name = "개발", channels = listOf(
            ProjectChannel.fromDataSource(
                id = DocumentId.from("ch2"),
                channelName = Name.from("개발"),
                order = ProjectChannelOrder.from(1),
                channelType = ProjectChannelType.MESSAGES,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ),
            ProjectChannel.fromDataSource(
                id = DocumentId.from("ch3"),
                channelName = Name.from("코드리뷰"),
                order = ProjectChannelOrder.from(2),
                channelType = ProjectChannelType.MESSAGES,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ),
            ProjectChannel.fromDataSource(
                id = DocumentId.from("ch4"),
                channelName = Name.from("회의"),
                order = ProjectChannelOrder.from(3),
                channelType = ProjectChannelType.TASKS,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        ), isExpanded = true // 확장된 상태
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            ExpandableCategoryItem(category = category, onCategoryClick = {}, onChannelClick = {})
        }
    }
}

@Preview(showBackground = true, name = "Expanded Category with Actions")
@Composable
private fun ExpandableCategoryItemExpandedActionsPreview() {
    val category = ExpandableCategoryData(
        id = "c2", name = "개발", channels = listOf(
            ProjectChannel.fromDataSource(
                id = DocumentId.from("ch5"),
                channelName = Name.from("테스트"),
                order = ProjectChannelOrder.from(1),
                channelType = ProjectChannelType.MESSAGES,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ),
            ProjectChannel.fromDataSource(
                id = DocumentId.from("ch6"),
                channelName = Name.from("배포"),
                order = ProjectChannelOrder.from(2),
                channelType = ProjectChannelType.MESSAGES,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        ), isExpanded = true // 확장된 상태
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            ExpandableCategoryItem(
                category = category,
                onCategoryClick = {},
                onChannelClick = {},
                showActions = true, // 액션 버튼 표시
                onCategoryEditClick = {},
                onCategoryDeleteClick = {},
                onAddChannelClick = {},
                onChannelEditClick = {},
                onChannelDeleteClick = {}
            )
        }
    }
}