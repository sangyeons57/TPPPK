package com.example.feature_main.ui.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tag
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
import com.example.domain.model.ChannelMode
import com.example.domain.model.ChannelType
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

// 누락된 모델 import 추가
import com.example.feature_main.ui.project.ProjectStructureUiState
import com.example.feature_main.ui.project.CategoryUiModel
import com.example.feature_main.ui.project.ChannelUiModel

/**
 * 프로젝트의 카테고리 및 채널 목록을 표시하는 컴포넌트
 */
@Composable
fun ProjectChannelList(
    structureUiState: ProjectStructureUiState,
    onCategoryClick: (CategoryUiModel) -> Unit,
    onChannelClick: (ChannelUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        // 일반 채널 (카테고리에 속하지 않은 채널)
        if (structureUiState.directChannel.isNotEmpty()) {
            items(structureUiState.directChannel) { channel ->
                ChannelItem(
                    channel = channel,
                    onClick = { onChannelClick(channel) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            }
        }
        
        // 카테고리 및 해당 채널 목록
        items(structureUiState.categories) { category ->
            CategoryItem(
                category = category,
                onCategoryClick = { onCategoryClick(category) },
                onChannelClick = onChannelClick,
                modifier = Modifier.fillMaxWidth()
            )
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
    onChannelClick: (ChannelUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 카테고리 헤더
        CategoryHeader(
            name = category.name,
            isExpanded = category.isExpanded,
            onClick = onCategoryClick
        )
        
        // 채널 목록 (펼쳐진 상태일 때만 표시)
        AnimatedVisibility(visible = category.isExpanded) {
            Column {
                category.channels.forEach { channel ->
                    ChannelItem(
                        channel = channel,
                        onClick = { onChannelClick(channel) },
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
    name: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        label = "CategoryArrowRotation"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            text = name.uppercase(),
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
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp), // 패딩 조정
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when(channel.mode) {
                ChannelMode.TEXT -> Icons.Default.Tag // Discord의 # 아이콘과 유사
                ChannelMode.VOICE -> Icons.Default.Mic
                ChannelMode.UNKNOWN -> Icons.Default.Tag // 기본값으로 Tag 아이콘
            },
            contentDescription = when(channel.mode) {
                ChannelMode.TEXT -> "텍스트 채널"
                ChannelMode.VOICE -> "음성 채널"
                ChannelMode.UNKNOWN -> "알 수 없는 채널"
            },
            modifier = Modifier.size(18.dp), // 아이콘 크기 약간 키움
            tint = iconColor
        )
        
        Spacer(modifier = Modifier.width(8.dp)) // 간격 조정
        
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = fontWeight),
            color = textColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis // 추가
        )
    }
}

// --- Previews Start ---

/**
 * ChannelItem 미리보기: 텍스트 채널, 선택됨
 */
@Preview(showBackground = true, name = "ChannelItem - Text Selected")
@Composable
fun ChannelItemPreview_TextSelected() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ChannelItem(
            channel = ChannelUiModel(id = "ch1", name = "일반 대화", mode = ChannelMode.TEXT, isSelected = true),
            onClick = {}
        )
    }
}

/**
 * ChannelItem 미리보기: 음성 채널, 선택 안됨
 */
@Preview(showBackground = true, name = "ChannelItem - Voice Unselected")
@Composable
fun ChannelItemPreview_VoiceUnselected() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ChannelItem(
            channel = ChannelUiModel(id = "ch2", name = "팀 보이스", mode = ChannelMode.VOICE, isSelected = false),
            onClick = {}
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
        CategoryHeader(name = "개발팀", isExpanded = true, onClick = {})
    }
}

/**
 * CategoryHeader 미리보기: 접힘
 */
@Preview(showBackground = true, name = "CategoryHeader - Collapsed")
@Composable
fun CategoryHeaderPreview_Collapsed() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CategoryHeader(name = "디자인팀", isExpanded = false, onClick = {})
    }
}

/**
 * CategoryItem 미리보기: 펼쳐짐 (내부 채널 포함)
 */
@Preview(showBackground = true, name = "CategoryItem - Expanded")
@Composable
fun CategoryItemPreview_Expanded() {
    val sampleChannels = listOf(
        ChannelUiModel(id = "ch3", name = "프론트엔드 논의", mode = ChannelMode.TEXT, isSelected = false),
        ChannelUiModel(id = "ch4", name = "백엔드 작업", mode = ChannelMode.TEXT, isSelected = true)
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CategoryItem(
            category = CategoryUiModel(id = "cat1", name = "엔지니어링", channels = sampleChannels, isExpanded = true),
            onCategoryClick = {},
            onChannelClick = {}
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
            category = CategoryUiModel(id = "cat2", name = "마케팅", channels = emptyList(), isExpanded = false),
            onCategoryClick = {},
            onChannelClick = {}
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
        ChannelUiModel(id = "gen1", name = "공지사항", mode = ChannelMode.TEXT, isSelected = false),
        ChannelUiModel(id = "gen2", name = "자유 게시판", mode = ChannelMode.TEXT, isSelected = false)
    )
    val categories = listOf(
        CategoryUiModel(
            id = "cat_dev", name = "개발팀", isExpanded = true,
            channels = listOf(
                ChannelUiModel(id = "dev_ch1", name = "프론트엔드", mode = ChannelMode.TEXT, isSelected = true),
                ChannelUiModel(id = "dev_ch2", name = "백엔드", mode = ChannelMode.TEXT, isSelected = false),
                ChannelUiModel(id = "dev_voice", name = "개발팀 음성", mode = ChannelMode.VOICE, isSelected = false)
            )
        ),
        CategoryUiModel(id = "cat_design", name = "디자인팀", isExpanded = false, channels = emptyList()),
        CategoryUiModel(
            id = "cat_plan", name = "기획팀", isExpanded = true,
            channels = listOf(
                ChannelUiModel(id = "plan_ch1", name = "아이디어 공유", mode = ChannelMode.TEXT, isSelected = false)
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
            onChannelClick = {}
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
            onChannelClick = {}
        )
    }
}

// TODO: ProjectStructureUiState, CategoryUiModel, ChannelUiModel 클래스 정의가 이 파일에 없다면
// 해당 모델들이 정의된 파일을 import하거나, 미리보기용으로 간단한 data class를 여기에 정의해야 합니다.
// 현재는 같은 파일 내에 있다고 가정하고 진행했습니다.
// ChannelMode도 import 필요 (com.example.domain.model.ChannelMode)
// TeamnovaPersonalProjectProjectingKotlinTheme import 필요 (com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme)

// --- Previews End --- 