package com.example.feature_chat.ui.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 사용자 프로필 다이얼로그 컴포넌트
 *
 * @param userId 사용자 ID
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param modifier Modifier
 */
@Composable
fun UserProfileDialog(
    userId: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("사용자 프로필") },
        text = { Text("사용자 ID: $userId\n(상세 정보 표시는 미구현)") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}
