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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .padding(vertical = 8.dp)
        ) {
            items(suggestions) { suggestion ->
                MentionSuggestionItem(
                    suggestion = suggestion,
                    onClick = { onSuggestionClick(suggestion) }
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
            // Role icon
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

        // Name and subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (suggestion.subtitle != null) {
                Text(
                    text = suggestion.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Type indicator
        Text(
            text = if (suggestion.type == MentionType.USER) "사용자" else "역할",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
