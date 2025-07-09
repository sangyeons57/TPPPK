package com.example.feature_friends.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core_ui.components.user.UserProfileImage
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserName
import com.example.feature_friends.viewmodel.FriendItem

/**
 * FriendManagementDialog: 친구 관리 옵션을 제공하는 바텀 시트 컨텐츠
 */
@Composable
fun FriendManagementDialog(
    friend: FriendItem,
    onRemoveFriend: (UserId) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRemoveConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 친구 정보 표시
        UserProfileImage(
            profileImageUrl = friend.profileImageUrl?.value,
            contentDescription = "${friend.displayName.value} 프로필",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = friend.displayName.value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "친구",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 관리 옵션들
        OutlinedButton(
            onClick = { showRemoveConfirmation = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.PersonRemove,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("친구 삭제")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("취소")
        }
        
        // 네비게이션 바 패딩
        Spacer(modifier = Modifier.height(16.dp))
    }

    // 친구 삭제 확인 다이얼로그
    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmation = false },
            title = { Text("친구 삭제") },
            text = { Text("${friend.displayName.value}님을 친구 목록에서 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveFriend(friend.friendId)
                        showRemoveConfirmation = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmation = false }) {
                    Text("취소")
                }
            }
        )
    }

}

@Preview(showBackground = true)
@Composable
private fun FriendManagementDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            FriendManagementDialog(
                friend = FriendItem(
                    user = null,
                    friendId = UserId.from("friend1"),
                    status = com.example.domain.model.enum.FriendStatus.ACCEPTED,
                    profileImageUrl = null,
                    requestedAt = null,
                    acceptedAt = null,
                    displayName = UserName.from("친구이름")
                ),
                onRemoveFriend = {},
                onDismiss = {}
            )
        }
    }
}