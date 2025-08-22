package com.example.feature_chat.ui.components.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.core_ui.components.user.SimpleUserProfileImage
import com.example.domain.vo.MentionType
import com.example.feature_chat.model.MentionSuggestion

/**
 * 멘션 제안 팝업 컴포넌트
 *
 * @param suggestions 멘션 제안 목록
 * @param onSuggestionClick 제안 클릭 콜백
 * @param modifier Modifier
 */
@Composable
fun MentionSuggestionsPopup(
    suggestions: List<MentionSuggestion>,
    onSuggestionClick: (MentionSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    maxVisibleItems: Int = 7,
    selectedIndex: Int = -1,
    query: String = "" // 검색 쿼리 하이라이팅용
) {
    Surface(
        modifier = modifier
            .zIndex(1f), // 입력창보다 위에 보이도록 보장
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        val itemHeight = 56 // dp per item (approx: 32 avatar + 24 paddings)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = (itemHeight * maxVisibleItems).dp)
                .padding(vertical = 8.dp)
        ) {
            itemsIndexed(suggestions) { index, suggestion ->
                MentionSuggestionItem(
                    suggestion = suggestion,
                    onClick = { onSuggestionClick(suggestion) },
                    isSelected = index == selectedIndex,
                    query = query
                )
            }
        }
    }
}

/**
 * 개별 멘션 제안 아이템 컴포넌트
 *
 * @param suggestion 멘션 제안 정보
 * @param onClick 클릭 콜백
 * @param modifier Modifier
 */
@Composable
fun MentionSuggestionItem(
    suggestion: MentionSuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    query: String = ""
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile or role icon
        if (suggestion.type == MentionType.USER) {
            SimpleUserProfileImage(
                imageUrl = suggestion.profileUrl,
                contentDescription = "${suggestion.displayName} 프로필",
                modifier = Modifier.size(32.dp)
            )
        } else {
            // Role or special mention icon
            if (suggestion.type == MentionType.EVERYONE) {
                // Special @everyone icon with distinct styling
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "모든 사용자",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // Regular role icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "@",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Name and subtitle with special styling for @everyone and query highlighting
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlightQuery(suggestion.displayName, query),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (suggestion.type == MentionType.EVERYONE) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (suggestion.type == MentionType.EVERYONE) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (suggestion.subtitle != null) {
                Text(
                    text = highlightQuery(suggestion.subtitle, query),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (suggestion.type == MentionType.EVERYONE) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // Type indicator with special label for @everyone
        Text(
            text = when (suggestion.type) {
                MentionType.USER -> "사용자"
                MentionType.EVERYONE -> "특수"
                else -> "역할"
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (suggestion.type == MentionType.EVERYONE) FontWeight.Medium else FontWeight.Normal
            ),
            color = if (suggestion.type == MentionType.EVERYONE) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    }
}

/**
 * 검색 쿼리를 하이라이팅한 AnnotatedString을 생성합니다
 */
@Composable
private fun highlightQuery(text: String, query: String): AnnotatedString {
    if (query.isBlank()) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var lastIndex = 0

        while (true) {
            val index = lowerText.indexOf(lowerQuery, lastIndex)
            if (index == -1) break

            // 쿼리 이전 텍스트 추가
            if (index > lastIndex) {
                append(text.substring(lastIndex, index))
            }

            // 쿼리 텍스트를 하이라이팅하여 추가
            withStyle(
                style = SpanStyle(
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(text.substring(index, index + query.length))
            }

            lastIndex = index + query.length
        }

        // 마지막 쿼리 이후 텍스트 추가
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}
