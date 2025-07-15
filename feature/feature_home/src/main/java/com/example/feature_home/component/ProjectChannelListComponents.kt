package com.example.feature_home.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.category.CategoryName
import com.example.feature_home.model.CategoryUiModel
import com.example.feature_home.model.ChannelUiModel
import com.example.feature_home.model.ProjectStructureUiState


/**
 * 프로젝트의 카테고리 및 채널 목록을 표시하는 컴포넌트
 */
@Composable
fun ProjectChannelList(
    structureUiState: ProjectStructureUiState,
    onCategoryClick: (CategoryUiModel) -> Unit,
    onCategoryLongPress: (CategoryUiModel) -> Unit,
    onChannelClick: (ChannelUiModel) -> Unit,
    onChannelLongPress: (ChannelUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        // 카테고리 및 해당 채널 목록을 먼저 표시
        items(structureUiState.categories) { category ->
            CategoryItem(
                category = category,
                onCategoryClick = { onCategoryClick(category) },
                onCategoryLongPress = { onCategoryLongPress(category) },
                onChannelClick = onChannelClick,
                onChannelLongPress = onChannelLongPress,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // 일반 채널 (카테고리에 속하지 않은 채널)을 아래쪽에 표시
        if (structureUiState.directChannel.isNotEmpty()) {
            item {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            }
            
            items(structureUiState.directChannel) { channel ->
                ChannelItem(
                    channel = channel,
                    onClick = { onChannelClick(channel) },
                    onLongPress = { onChannelLongPress(channel) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 카테고리 아이템 컴포넌트
 */
@Composable
fun CategoryItem(
    category: CategoryUiModel,
    onCategoryClick: () -> Unit,
    onCategoryLongPress: () -> Unit,
    onChannelClick: (ChannelUiModel) -> Unit,
    onChannelLongPress: (ChannelUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 카테고리 헤더
        CategoryHeader(
            name = category.name,
            isExpanded = category.isExpanded,
            onClick = onCategoryClick,
            onLongClick = onCategoryLongPress
        )
        
        // 채널 목록 (펼쳐진 상태일 때만 표시)
        AnimatedVisibility(visible = category.isExpanded) {
            Column {
                category.channels.forEach { channel ->
                    ChannelItem(
                        channel = channel,
                        onClick = { onChannelClick(channel) },
                        onLongPress = { onChannelLongPress(channel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp) // 들여쓰기 효과
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 카테고리 헤더 컴포넌트
 */
@Composable
fun CategoryHeader(
    name: CategoryName,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        label = "CategoryArrowRotation"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "카테고리 접기" else "카테고리 펼치기",
            modifier = Modifier
                .size(16.dp)
                .rotate(arrowRotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.width(6.dp))
        
        Text(
            text = name.value.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

/**
 * 채널 아이템 컴포넌트
 */
@Composable
fun ChannelItem(
    channel: ChannelUiModel,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = if (channel.isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer // 선택 시 아이콘 색상
    else
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) // 기본 아이콘 색상 (Discord 회색톤과 유사하게)

    val textColor = if (channel.isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer // 선택 시 텍스트 색상
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) // 기본 텍스트 색상 (Discord 회색톤과 유사하게)

    val fontWeight = if (channel.isSelected)
        FontWeight.SemiBold // 선택 시 굵게
    else
        FontWeight.Normal

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                color = if (channel.isSelected)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) // 선택 시 배경 (연한 강조색)
                else
                    Color.Transparent
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 8.dp, horizontal = 12.dp), // 패딩 조정
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when(channel.mode) {
                ProjectChannelType.MESSAGES -> Icons.Default.Tag // 메시지 채널: # 태그 아이콘
                ProjectChannelType.TASKS -> Icons.Default.CheckCircle // 테스크 채널: 체크 아이콘
                else -> Icons.Default.Tag // 기본값으로 Tag 아이콘
            },
            contentDescription = when(channel.mode) {
                ProjectChannelType.MESSAGES -> "메시지 채널"
                ProjectChannelType.TASKS -> "테스크 채널"
                else -> "알 수 없는 채널"
            },
            modifier = Modifier.size(18.dp), // 아이콘 크기 약간 키움
            tint = iconColor
        )
        
        Spacer(modifier = Modifier.width(8.dp)) // 간격 조정
        
        Text(
            text = channel.name.value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = fontWeight),
            color = textColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis // 추가
        )
    }
}

// --- Previews Start ---

/**
 * ChannelItem 미리보기: 메시지 채널, 선택됨
 */
@Preview(showBackground = true, name = "ChannelItem - Message Selected")
@Composable
fun ChannelItemPreview_MessageSelected() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ChannelItem(
            channel = ChannelUiModel(
                id = DocumentId("ch1"),
                name = Name("일반 대화"),
                mode = ProjectChannelType.MESSAGES,
                isSelected = true
            ),
            onClick = {},
            onLongPress = {}
        )
    }
}

/**
 * ChannelItem 미리보기: 테스크 채널, 선택 안됨
 */
@Preview(showBackground = true, name = "ChannelItem - Task Unselected")
@Composable
fun ChannelItemPreview_TaskUnselected() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ChannelItem(
            channel = ChannelUiModel(
                id = DocumentId("ch2"),
                name = Name("할 일 관리"),
                mode = ProjectChannelType.TASKS,
                isSelected = false
            ),
            onClick = {},
            onLongPress = {}
        )
    }
}


/**
 * CategoryHeader 미리보기: 펼쳐짐
 */
@Preview(showBackground = true, name = "CategoryHeader - Expanded")
@Composable
fun CategoryHeaderPreview_Expanded() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CategoryHeader(
            name = CategoryName("개발팀"),
            isExpanded = true,
            onClick = {},
            onLongClick = {})
    }
}

/**
 * CategoryHeader 미리보기: 접힘
 */
@Preview(showBackground = true, name = "CategoryHeader - Collapsed")
@Composable
fun CategoryHeaderPreview_Collapsed() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CategoryHeader(
            name = CategoryName("디자인팀"),
            isExpanded = false,
            onClick = {},
            onLongClick = {})
    }
}

/**
 * CategoryItem 미리보기: 펼쳐짐 (내부 채널 포함)
 */
@Preview(showBackground = true, name = "CategoryItem - Expanded")
@Composable
fun CategoryItemPreview_Expanded() {
    val sampleChannels = listOf(
        ChannelUiModel(
            id = DocumentId("ch3"),
            name = Name("프론트엔드 논의"),
            mode = ProjectChannelType.MESSAGES,
            isSelected = false
        ),
        ChannelUiModel(
            id = DocumentId("ch4"),
            name = Name("백엔드 작업"),
            mode = ProjectChannelType.MESSAGES,
            isSelected = true
        ),
        ChannelUiModel(
            id = DocumentId("ch5"),
            name = Name("스프린트 관리"),
            mode = ProjectChannelType.TASKS,
            isSelected = false
        )
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CategoryItem(
            category = CategoryUiModel(
                id = DocumentId("ch1"),
                name = CategoryName("엔지니어링"),
                order = 1.0,
                channels = sampleChannels,
                isExpanded = true
            ),
            onCategoryClick = {},
            onCategoryLongPress = {},
            onChannelClick = {},
            onChannelLongPress = {}
        )
    }
}

/**
 * CategoryItem 미리보기: 접힘
 */
@Preview(showBackground = true, name = "CategoryItem - Collapsed")
@Composable
fun CategoryItemPreview_Collapsed() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CategoryItem(
            category = CategoryUiModel(
                id = DocumentId("cat2"),
                name = CategoryName("마케팅"),
                order = 2.0,
                channels = emptyList(),
                isExpanded = false
            ),
            onCategoryClick = {},
            onCategoryLongPress = {},
            onChannelClick = {},
            onChannelLongPress = {}
        )
    }
}

/**
 * ProjectChannelList 미리보기: 기본 상태 (일반 채널, 카테고리 포함)
 */
@Preview(showBackground = true, name = "ProjectChannelList - Default")
@Composable
fun ProjectChannelListPreview_Default() {
    val generalChannels = listOf(
        ChannelUiModel(
            id = DocumentId("gen1"),
            name = Name("공지사항"),
            mode = ProjectChannelType.MESSAGES,
            isSelected = false
        ),
        ChannelUiModel(
            id = DocumentId("gen2"),
            name = Name("자유 게시판"),
            mode = ProjectChannelType.MESSAGES,
            isSelected = false
        ),
        ChannelUiModel(
            id = DocumentId("gen3"),
            name = Name("전체 할 일"),
            mode = ProjectChannelType.TASKS,
            isSelected = false
        )
    )
    val categories = listOf(
        CategoryUiModel(
            id = DocumentId("cat_dev"), name = CategoryName("개발팀"), order = 1.0, isExpanded = true,
            channels = listOf(
                ChannelUiModel(
                    id = DocumentId("dev_ch1"),
                    name = Name("프론트엔드"),
                    mode = ProjectChannelType.MESSAGES,
                    isSelected = true
                ),
                ChannelUiModel(
                    id = DocumentId("dev_ch2"),
                    name = Name("백엔드"),
                    mode = ProjectChannelType.MESSAGES,
                    isSelected = false
                ),
                ChannelUiModel(
                    id = DocumentId("dev_tasks"),
                    name = Name("개발 작업"),
                    mode = ProjectChannelType.TASKS,
                    isSelected = false
                )
            )
        ),
        CategoryUiModel(
            id = DocumentId("cat_design"),
            name = CategoryName("디자인팀"),
            order = 2.0,
            isExpanded = false,
            channels = emptyList()
        ),
        CategoryUiModel(
            id = DocumentId("cat_plan"), name = CategoryName("기획팀"), order = 3.0, isExpanded = true,
            channels = listOf(
                ChannelUiModel(
                    id = DocumentId("plan_ch1"),
                    name = Name("아이디어 공유"),
                    mode = ProjectChannelType.MESSAGES,
                    isSelected = false
                )
            )
        )
    )
    val uiState = ProjectStructureUiState(
        isLoading = false,
        error = null,
        categories = categories,
        directChannel = generalChannels
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProjectChannelList(
            structureUiState = uiState,
            onCategoryClick = {},
            onCategoryLongPress = {},
            onChannelClick = {},
            onChannelLongPress = {}
        )
    }
}

/**
 * ProjectChannelList 미리보기: 비어 있는 상태
 */
@Preview(showBackground = true, name = "ProjectChannelList - Empty")
@Composable
fun ProjectChannelListPreview_Empty() {
    val uiState = ProjectStructureUiState(
        isLoading = false,
        error = null,
        categories = emptyList(),
        directChannel = emptyList()
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProjectChannelList(
            structureUiState = uiState,
            onCategoryClick = {},
            onCategoryLongPress = {},
            onChannelClick = {},
            onChannelLongPress = {}
        )
    }
}

// TODO: ProjectStructureUiState, CategoryUiModel, ChannelUiModel 클래스 정의가 이 파일에 없다면
// 해당 모델들이 정의된 파일을 import하거나, 미리보기용으로 간단한 data class를 여기에 정의해야 합니다.
// 현재는 같은 파일 내에 있다고 가정하고 진행했습니다.
// ChannelMode도 import 필요 (com.example.domain.model.ChannelMode)
// TeamnovaPersonalProjectProjectingKotlinTheme import 필요 (com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme)

// --- Previews End --- 