package com.example.feature_main.ui.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ChannelType

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
        if (structureUiState.generalChannels.isNotEmpty()) {
            items(structureUiState.generalChannels) { channel ->
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
                        .padding(vertical = 8.dp),
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
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "카테고리 접기" else "카테고리 펼치기",
            modifier = Modifier
                .size(16.dp)
                .rotate(arrowRotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = name.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                color = if (channel.isSelected) 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else
                    Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 채널 타입에 따른 아이콘
        Icon(
            imageVector = when(channel.type) {
                ChannelType.TEXT -> Icons.Default.Tag
                ChannelType.VOICE -> Icons.Default.Mic
            },
            contentDescription = when(channel.type) {
                ChannelType.TEXT -> "텍스트 채널"
                ChannelType.VOICE -> "음성 채널"
            },
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.width(6.dp))
        
        // 채널 이름
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (channel.isSelected)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
} 