package com.example.feature_chat.ui.components

/** 다른 문제 전부 해결하고나면 VIewmodel구현 왜냐하면 chat은 다음주에 할일 이기때문
import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.base.Message

/**
 * 채팅 메시지 아이템 컴포저블
 * 메시지 내용, 리액션, 시간 등을 표시합니다.
 *
 * @param message 표시할 메시지 객체
 * @param isCurrentUser 현재 사용자의 메시지인지 여부
 * @param currentUserId 현재 로그인한 사용자 ID
 * @param onReactionClick 이모티콘 리액션 클릭 이벤트
 * @param onLongPress 메시지 롱프레스 이벤트 (수정/삭제 메뉴 등)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: Message,
    isCurrentUser: Boolean,
    currentUserId: String,
    onReactionClick: (String) -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        // 메시지 버블
        Box(
            modifier = Modifier
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress
                )
                .background(
                    color = if (isCurrentUser) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else 
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.content,
                color = if (isCurrentUser) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 메시지 시간 및 상태
        Row(
            modifier = Modifier.padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DateUtils.formatDateTime(
                    LocalContext.current,
                    message.updatedAt!!.epochSecond * 1000,
                    DateUtils.FORMAT_SHOW_TIME
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            if (isCurrentUser && message.isEdited) {
                Text(
                    text = " · 수정됨",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        
        // 리액션 표시
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                message.reactions.forEach { (reaction, users) ->
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                color = if (users.contains(currentUserId))
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onReactionClick(reaction) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = reaction)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = users.size.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
        */