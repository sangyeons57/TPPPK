package com.example.feature_chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 메시지 입력 컴포넌트
 * 텍스트 입력, 첨부파일 추가, 메시지 전송 등의 기능을 제공합니다.
 *
 * @param text 입력 텍스트
 * @param isEditing 메시지 수정 모드 여부
 * @param onTextChange 텍스트 변경 이벤트
 * @param onSendClick 전송 버튼 클릭 이벤트
 * @param onAttachmentClick 첨부파일 버튼 클릭 이벤트
 * @param onCancelEdit 수정 취소 이벤트
 */
@Composable
fun MessageInput(
    text: String,
    isEditing: Boolean = false,
    onTextChange: (String) -> Unit = {},
    onSendClick: () -> Unit = {},
    onAttachmentClick: () -> Unit = {},
    onCancelEdit: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // 수정 모드 표시줄
            AnimatedVisibility(visible = isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Text(
                        text = "메시지 수정 중",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    
                    IconButton(
                        onClick = onCancelEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "수정 취소",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // 메시지 입력 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 첨부파일 버튼
                IconButton(
                    onClick = onAttachmentClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "첨부파일 추가",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 텍스트 입력창
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    placeholder = { 
                        Text(if (isEditing) "메시지 수정..." else "메시지 입력...") 
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = false,
                    maxLines = 5
                )
                
                // 전송 버튼
                Button(
                    onClick = onSendClick,
                    enabled = text.isNotBlank(),
                    shape = CircleShape,
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isEditing) "수정 완료" else "메시지 전송"
                    )
                }
            }
        }
    }
} 