package com.example.feature_chat.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.feature_chat.model.ChatMessageUiModel

/**
 * 메시지 수정/삭제 다이얼로그 컴포넌트
 *
 * @param message 메시지 정보
 * @param isMyMessage 내 메시지인지 여부
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onEdit 수정 버튼 클릭 콜백
 * @param onDelete 삭제 버튼 클릭 콜백
 * @param modifier Modifier
 */
@Composable
fun EditDeleteChatDialog(
    message: ChatMessageUiModel,
    isMyMessage: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("메시지 옵션") },
        text = {
            Text(
                "\"${message.message.take(30)}${if (message.message.length > 30) "..." else ""}\"",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        confirmButton = {
            if (isMyMessage) { // 내 메시지일 경우에만 수정/삭제 버튼 표시
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_message_option")
                    ) {
                        Text("수정")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("delete_message_option")
                    ) {
                        Text("삭제")
                    }
                }
            } else { // 다른 사람 메시지면 확인 버튼만 (또는 신고 버튼 등 추가 가능)
                TextButton(onClick = onDismiss) {
                    Text("확인")
                }
            }
        },
        dismissButton = {
            if (isMyMessage) { // 내 메시지일 경우 취소 버튼
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
            // 다른 사람 메시지면 dismiss 버튼 불필요
        }
    )
}
