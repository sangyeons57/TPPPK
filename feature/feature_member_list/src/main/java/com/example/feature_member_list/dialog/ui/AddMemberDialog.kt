package com.example.feature_member_list.dialog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.components.user.UserProfileImage
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserName
import com.example.feature_member_list.dialog.viewmodel.AddMemberViewModel
import com.example.feature_member_list.dialog.viewmodel.AddMemberDialogEvent
import kotlinx.coroutines.flow.collectLatest

/**
 * 친구 정보를 나타내는 UI 모델
 */
data class FriendItem(
    val userId: UserId,
    val userName: UserName,
    val userEmail: String?,
    val profileImageUrl: String?,
    val isOnline: Boolean = false // 온라인 상태 (나중에 추가 가능)
)

/**
 * 프로젝트에 멤버를 초대하는 다이얼로그 Composable
 * 두 가지 방식을 제공:
 * 1. 프로젝트 참가 링크 복사
 * 2. 친구 목록에서 선택하여 초대
 *
 * @param onDismissRequest 다이얼로그 닫기 요청 콜백
 * @param onFriendsInvited 선택된 친구들에게 초대 요청 후 콜백
 * @param projectInviteLink 프로젝트 참가 링크 (null이면 생성 중)
 * @param friends 친구 목록
 * @param selectedFriends 현재 선택된 친구들
 * @param onFriendSelectionChange 친구 선택/해제 콜백
 * @param onGenerateInviteLink 초대 링크 생성 요청 콜백
 * @param onCopyInviteLink 초대 링크 복사 요청 콜백
 * @param isLoadingLink 링크 생성 로딩 상태
 * @param isLoadingFriends 친구 목록 로딩 상태
 * @param error 에러 메시지
 */
@Composable
fun AddMemberDialogContent(
    onDismissRequest: () -> Unit,
    onFriendsInvited: (Set<UserId>) -> Unit,
    projectInviteLink: String?, // 생성된 프로젝트 참가 링크
    friends: List<FriendItem>,
    selectedFriends: Set<UserId>,
    onFriendSelectionChange: (UserId, Boolean) -> Unit,
    onGenerateInviteLink: () -> Unit,
    onCopyInviteLink: (String) -> Unit,
    isLoadingLink: Boolean,
    isLoadingFriends: Boolean,
    error: String?
) {
    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 400.dp, max = 700.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 제목
                Text(
                    "멤버 초대",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // 1️⃣ 프로젝트 참가 링크 섹션
                InviteLinkSection(
                    inviteLink = projectInviteLink,
                    isLoading = isLoadingLink,
                    onGenerateLink = onGenerateInviteLink,
                    onCopyLink = { link ->
                        clipboardManager.setText(AnnotatedString(link))
                        onCopyInviteLink(link)
                    }
                )

                Divider()

                // 2️⃣ 친구 초대 섹션
                FriendInviteSection(
                    friends = friends,
                    selectedFriends = selectedFriends,
                    onFriendSelectionChange = onFriendSelectionChange,
                    isLoading = isLoadingFriends,
                    error = error,
                    modifier = Modifier.weight(1f)
                )

                // 하단 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onFriendsInvited(selectedFriends) },
                        enabled = selectedFriends.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("친구 초대 (${selectedFriends.size})")
                    }
                }
            }
        }
    }
}

/**
 * 프로젝트 참가 링크 섹션
 */
@Composable
private fun InviteLinkSection(
    inviteLink: String?,
    isLoading: Boolean,
    onGenerateLink: () -> Unit,
    onCopyLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "프로젝트 참가 링크",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("링크 생성 중...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            inviteLink != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            inviteLink,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onCopyLink(inviteLink) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("복사")
                            }
                            OutlinedButton(onClick = onGenerateLink) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("새로 생성")
                            }
                        }
                    }
                }
            }
            
            else -> {
                OutlinedButton(
                    onClick = onGenerateLink,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("초대 링크 생성")
                }
            }
        }
    }
}

/**
 * 친구 초대 섹션
 */
@Composable
private fun FriendInviteSection(
    friends: List<FriendItem>,
    selectedFriends: Set<UserId>,
    onFriendSelectionChange: (UserId, Boolean) -> Unit,
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.PersonAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "친구 초대",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "오류: $error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                friends.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "초대할 수 있는 친구가 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = friends,
                            key = { it.userId.value }
                        ) { friend ->
                            FriendInviteItem(
                                friend = friend,
                                isSelected = friend.userId in selectedFriends,
                                onSelectionChange = { isSelected ->
                                    onFriendSelectionChange(friend.userId, isSelected)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 개별 친구 초대 아이템
 */
@Composable
private fun FriendInviteItem(
    friend: FriendItem,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelectionChange(!isSelected) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserProfileImage(
            userId = friend.userId.value,
            contentDescription = "${friend.userName.value} 프로필",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.userName.value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            friend.userEmail?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChange
        )
    }
}

/**
 * 멤버 추가 다이얼로그 컴포넌트 (ViewModel 연동)
 */
@Composable
fun AddMemberDialog(
    projectId: DocumentId,
    onDismissRequest: () -> Unit,
    onMemberAdded: () -> Unit,
    viewModel: AddMemberViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddMemberDialogEvent.ShowSnackbar -> {
                    // 스낵바 표시 (부모에서 처리)
                    println("Snackbar: ${event.message}")
                }
                AddMemberDialogEvent.DismissDialog -> onDismissRequest()
                AddMemberDialogEvent.MembersAddedSuccessfully -> {
                    onMemberAdded()
                }
            }
        }
    }

    LaunchedEffect(projectId) {
        viewModel.loadProjectInviteLink(projectId)
        viewModel.loadFriends()
    }

    AddMemberDialogContent(
        onDismissRequest = onDismissRequest,
        onFriendsInvited = { selectedFriends ->
            viewModel.inviteFriends(projectId, selectedFriends)
        },
        projectInviteLink = uiState.projectInviteLink,
        friends = uiState.friends,
        selectedFriends = uiState.selectedFriends,
        onFriendSelectionChange = viewModel::onFriendSelectionChanged,
        onGenerateInviteLink = { viewModel.generateProjectInviteLink(projectId) },
        onCopyInviteLink = { link -> viewModel.onInviteLinkCopied(link) },
        isLoadingLink = uiState.isLoadingLink,
        isLoadingFriends = uiState.isLoadingFriends,
        error = uiState.error
    )
}

/**
 * 미리보기: 새로운 멤버 초대 다이얼로그
 */
@Preview(showBackground = true)
@Composable
fun AddMemberDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AddMemberDialogContent(
            onDismissRequest = {},
            onFriendsInvited = {},
            projectInviteLink = "https://projecting.app/join/abc123def456",
            friends = listOf(
                FriendItem(
                    userId = UserId("friend1"),
                    userName = UserName("김영희"),
                    userEmail = "kim@example.com",
                    profileImageUrl = null
                ),
                FriendItem(
                    userId = UserId("friend2"),
                    userName = UserName("박철수"),
                    userEmail = "park@example.com",
                    profileImageUrl = null
                )
            ),
            selectedFriends = setOf(UserId("friend1")),
            onFriendSelectionChange = { _, _ -> },
            onGenerateInviteLink = {},
            onCopyInviteLink = {},
            isLoadingLink = false,
            isLoadingFriends = false,
            error = null
        )
    }
} 