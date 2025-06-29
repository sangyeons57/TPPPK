package com.example.feature_member.dialog.ui

// Removed direct Coil imports
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
import com.example.feature_member.dialog.viewmodel.AddMemberDialogEvent
import com.example.feature_member.dialog.viewmodel.AddMemberViewModel
import kotlinx.coroutines.flow.collectLatest

// R import might be removed if UserProfileImage handles it all.

/**
 * 사용자 검색 결과를 표현하는 데이터 클래스
 * 
 * @param userId 사용자 ID
 * @param userName 사용자 이름
 * @param userEmail 사용자 이메일 (nullable)
 * @param profileImageUrl 프로필 이미지 URL (nullable)
 */
data class UserSearchResult(
    val userId: UserId,
    val userName: UserName,
    val userEmail: String?,
    val profileImageUrl: String?
)

/**
 * 프로젝트에 멤버를 검색하고 추가하는 다이얼로그 Composable
 *
 * @param onDismissRequest 다이얼로그 닫기 요청 콜백
 * @param onAddMembers 선택된 사용자 목록(ID)으로 멤버 추가 요청 콜백
 * @param username 현재 검색어 상태
 * @param onSearchQueryChange 검색어 변경 콜백
 * @param searchResults 사용자 검색 결과 목록 상태
 * @param selectedUsers 현재 선택된 사용자 ID Set 상태
 * @param onUserSelectionChange 사용자 선택/해제 콜백 (userId, isSelected)
 * @param isLoading 검색 로딩 상태
 * @param error 검색 에러 메시지
 */
// Renamed from AddMemberDialog to AddMemberDialogContent
@Composable
fun AddMemberDialogContent(
    onDismissRequest: () -> Unit,
    onAddMembers: (Set<UserId>) -> Unit, // This will be called by the ViewModel-driven AddMemberDialog
    username: UserName,
    onSearchQueryChange: (UserName) -> Unit,
    searchResults: List<UserSearchResult>,
    selectedUsers: Set<UserId>,
    onUserSelectionChange: (UserId, Boolean) -> Unit,
    isLoading: Boolean,
    error: String?
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("멤버 초대", style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username.value,
                    onValueChange = { value -> onSearchQueryChange(UserName(value)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text("이름 또는 이메일로 검색") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (username.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange(UserName.EMPTY) }) {
                                Icon(Icons.Filled.Clear, contentDescription = "지우기")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                    })
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        isLoading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        error != null -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("오류: $error", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        username.isNotBlank() && searchResults.isEmpty() && !isLoading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("검색 결과가 없습니다.", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        else -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(
                                    items = searchResults,
                                    key = { it.userId }
                                ) { user ->
                                    UserSearchResultItem(
                                        user = user,
                                        isSelected = user.userId in selectedUsers,
                                        onCheckedChange = { isChecked ->
                                            onUserSelectionChange(user.userId, isChecked)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAddMembers(selectedUsers) },
                        enabled = selectedUsers.isNotEmpty()
                    ) {
                        Text("추가")
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * 사용자 검색 결과 목록의 각 항목 UI
 * 
 * @param user 표시할 사용자 검색 결과 정보
 * @param isSelected 사용자가 선택되었는지 여부
 * @param onCheckedChange 선택 상태 변경 콜백
 * @param modifier 커스텀 모디파이어
 */
@Composable
fun UserSearchResultItem(
    user: UserSearchResult,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isSelected) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserProfileImage(
            profileImageUrl = user.profileImageUrl,
            contentDescription = "${user.userName} 프로필",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
            // contentScale is handled by UserProfileImage by default
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = user.userName.value,
                style = MaterialTheme.typography.bodyLarge
            )
            user.userEmail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * 미리보기: 멤버 추가 다이얼로그
 */
@Preview(showBackground = true)
@Composable
fun AddMemberDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        // Preview now calls AddMemberDialogContent directly
        AddMemberDialogContent(
            onDismissRequest = {},
            onAddMembers = {},
            username = UserName.EMPTY,
            onSearchQueryChange = {},
            searchResults = listOf(
                UserSearchResult(
                    userId = UserId("user1"),
                    userName = UserName("김영희"),
                    userEmail = "kim@example.com",
                    profileImageUrl = null
                ),
                UserSearchResult(
                    userId = UserId("user2"),
                    userName = UserName("김철수"),
                    userEmail = "kim2@example.com",
                    profileImageUrl = null
                )
            ),
            selectedUsers = setOf(UserId("user1")),
            onUserSelectionChange = { _, _ -> },
            isLoading = false,
            error = null
        )
    }
}


/**
 * 멤버 추가 다이얼로그 컴포넌트의 간편한 버전
 * 
 * @param projectId 프로젝트 ID
 * @param onDismissRequest 다이얼로그 닫기 요청 콜백
 * @param onMemberAdded 멤버 추가 완료 후 콜백
 */
// This is the new AddMemberDialog that uses the ViewModel
@Composable
fun AddMemberDialog( // This is the one called by MemberListScreen
    projectId: DocumentId,
    onDismissRequest: () -> Unit,
    onMemberAdded: () -> Unit, // Callback when members are successfully added
    viewModel: AddMemberViewModel = hiltViewModel() // Inject ViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // val snackbarHostState = remember { SnackbarHostState() } // Requires Scaffold or another SnackbarHost

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddMemberDialogEvent.ShowSnackbar -> {
                    // snackbarHostState.showSnackbar(event.message) // Requires SnackbarHostState
                    println("Snackbar: ${event.message}") // Placeholder
                }
                AddMemberDialogEvent.DismissDialog -> onDismissRequest() // Dialog might be dismissed by parent too
                AddMemberDialogEvent.MembersAddedSuccessfully -> {
                    onMemberAdded() // Call parent's callback
                    // onDismissRequest() // Parent screen (MemberListScreen) will handle dismissal
                }
            }
        }
    }

    // Use the more detailed AddMemberDialogContent composable, passing state and event handlers
    AddMemberDialogContent(
        onDismissRequest = onDismissRequest,
        onAddMembers = { _ -> // selectedUserIds Set<String> - ViewModel handles selected users
            viewModel.addSelectedMembers(projectId) // Pass default roles if any, e.g., emptyList()
        },
        username = uiState.username,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        searchResults = listOf(uiState.searchResults!!),
        selectedUsers = uiState.selectedUsers,
        onUserSelectionChange = viewModel::onUserSelectionChanged,
        isLoading = uiState.isLoading,
        error = uiState.error
    )
} 
// Removed the simpler, overloaded AddMemberDialog function with dummy logic