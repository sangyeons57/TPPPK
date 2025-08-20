package com.example.feature_member_list.dialog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.components.user.UserProfileImage
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.user.UserName
import com.example.feature_member_list.dialog.viewmodel.AddMemberDialogEvent
import com.example.feature_member_list.dialog.viewmodel.AddMemberViewModel
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
 * 1. 친구 목록에서 선택하여 초대
 * 2. 사용자 이름으로 직접 검색하여 초대
 *
 * @param onDismissRequest 다이얼로그 닫기 요청 콜백
 * @param onMembersInvited 선택된 사용자들에게 초대 요청 후 콜백
 * @param friends 친구 목록
 * @param selectedMembers 현재 선택된 멤버들 (친구 + 검색된 사용자)
 * @param onMemberSelectionChange 멤버 선택/해제 콜백
 * @param searchQuery 사용자 이름 검색 쿼리
 * @param onSearchQueryChange 검색 쿼리 변경 콜백
 * @param searchedUsers 검색된 사용자 목록
 * @param onSearchUser 사용자 검색 요청 콜백
 * @param isLoadingFriends 친구 목록 로딩 상태
 * @param isLoadingSearch 사용자 검색 로딩 상태
 * @param error 에러 메시지
 */
/**
 * 검색된 사용자 정보를 나타내는 UI 모델
 */
data class SearchedUser(
    val userId: UserId,
    val userName: UserName,
    val userEmail: String?,
    val profileImageUrl: String?
)

@Composable
fun AddMemberDialogContent(
    onDismissRequest: () -> Unit,
    onMembersInvited: (Set<UserId>) -> Unit,
    friends: List<FriendItem>,
    selectedMembers: Set<UserId>,
    onMemberSelectionChange: (UserId, Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchedUsers: List<SearchedUser>,
    onSearchUser: (String) -> Unit,
    isLoadingFriends: Boolean,
    isLoadingSearch: Boolean,
    error: String?
) {

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

                // 1️⃣ 사용자 이름 검색 섹션
                UserSearchSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearchUser = onSearchUser,
                    searchedUsers = searchedUsers,
                    selectedMembers = selectedMembers,
                    onMemberSelectionChange = onMemberSelectionChange,
                    isLoadingSearch = isLoadingSearch
                )

                // 2️⃣ 친구 초대 섹션
                MemberInviteSection(
                    friends = friends,
                    selectedMembers = selectedMembers,
                    onMemberSelectionChange = onMemberSelectionChange,
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
                        onClick = { onMembersInvited(selectedMembers) },
                        enabled = selectedMembers.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("멤버 초대 (${selectedMembers.size})")
                    }
                }
            }
        }
    }
}

/**
 * 사용자 이름 검색 섹션
 */
@Composable
private fun UserSearchSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchUser: (String) -> Unit,
    searchedUsers: List<SearchedUser>,
    selectedMembers: Set<UserId>,
    onMemberSelectionChange: (UserId, Boolean) -> Unit,
    isLoadingSearch: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "이름으로 검색",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 검색 입력 필드
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("사용자 이름을 입력하세요") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = { onSearchUser(searchQuery) },
                enabled = searchQuery.isNotBlank() && !isLoadingSearch
            ) {
                if (isLoadingSearch) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Filled.Search, contentDescription = null)
                }
            }
        }

        // 검색 결과 표시
        if (searchedUsers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "검색 결과",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            searchedUsers.forEach { user ->
                UserInviteItem(
                    userId = user.userId,
                    userName = user.userName.value,
                    userEmail = user.userEmail,
                    profileImageUrl = user.profileImageUrl,
                    isSelected = user.userId in selectedMembers,
                    onSelectionChange = { isSelected ->
                        onMemberSelectionChange(user.userId, isSelected)
                    }
                )
            }
        }
    }
}

/**
 * 멤버 초대 섹션 (친구 목록)
 */
@Composable
private fun MemberInviteSection(
    friends: List<FriendItem>,
    selectedMembers: Set<UserId>,
    onMemberSelectionChange: (UserId, Boolean) -> Unit,
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
                "친구 목록에서 초대",
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
                            "친구 목록이 비어있습니다.",
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
                            UserInviteItem(
                                userId = friend.userId,
                                userName = friend.userName.value,
                                userEmail = friend.userEmail,
                                profileImageUrl = friend.profileImageUrl,
                                isSelected = friend.userId in selectedMembers,
                                onSelectionChange = { isSelected ->
                                    onMemberSelectionChange(friend.userId, isSelected)
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
 * 개별 사용자 초대 아이템 (친구 및 검색된 사용자)
 */
@Composable
private fun UserInviteItem(
    userId: UserId,
    userName: String,
    userEmail: String?,
    profileImageUrl: String?,
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
            userId = userId.value,
            contentDescription = "$userName 프로필",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            viewModel = hiltViewModel(key = userId.value)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = userName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            userEmail?.let { email ->
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
        viewModel.loadFriends()
    }

    AddMemberDialogContent(
        onDismissRequest = onDismissRequest,
        onMembersInvited = { selectedMembers ->
            viewModel.inviteMembers(projectId, selectedMembers)
        },
        friends = uiState.friends,
        selectedMembers = uiState.selectedMembers,
        onMemberSelectionChange = viewModel::onMemberSelectionChanged,
        searchQuery = uiState.searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        searchedUsers = uiState.searchedUsers,
        onSearchUser = viewModel::searchUserByName,
        isLoadingFriends = uiState.isLoadingFriends,
        isLoadingSearch = uiState.isLoadingSearch,
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
            onMembersInvited = {},
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
            selectedMembers = setOf(UserId("friend1")),
            onMemberSelectionChange = { _, _ -> },
            searchQuery = "",
            onSearchQueryChange = {},
            searchedUsers = listOf(
                SearchedUser(
                    userId = UserId("search1"),
                    userName = UserName("이지은"),
                    userEmail = "lee@example.com",
                    profileImageUrl = null
                )
            ),
            onSearchUser = {},
            isLoadingFriends = false,
            isLoadingSearch = false,
            error = null
        )
    }
}
