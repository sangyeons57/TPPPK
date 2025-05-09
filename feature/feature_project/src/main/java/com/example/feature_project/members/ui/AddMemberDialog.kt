package com.example.feature_project.members.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.core_ui.R

/**
 * 사용자 검색 결과를 표현하는 데이터 클래스
 * 
 * @param userId 사용자 ID
 * @param userName 사용자 이름
 * @param userEmail 사용자 이메일 (nullable)
 * @param profileImageUrl 프로필 이미지 URL (nullable)
 */
data class UserSearchResult(
    val userId: String,
    val userName: String,
    val userEmail: String?,
    val profileImageUrl: String?
)

/**
 * 프로젝트에 멤버를 검색하고 추가하는 다이얼로그 Composable
 *
 * @param onDismissRequest 다이얼로그 닫기 요청 콜백
 * @param onAddMembers 선택된 사용자 목록(ID)으로 멤버 추가 요청 콜백
 * @param searchQuery 현재 검색어 상태
 * @param onSearchQueryChange 검색어 변경 콜백
 * @param searchResults 사용자 검색 결과 목록 상태
 * @param selectedUsers 현재 선택된 사용자 ID Set 상태
 * @param onUserSelectionChange 사용자 선택/해제 콜백 (userId, isSelected)
 * @param isLoading 검색 로딩 상태
 * @param error 검색 에러 메시지
 */
@Composable
fun AddMemberDialog(
    onDismissRequest: () -> Unit,
    onAddMembers: (Set<String>) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<UserSearchResult>,
    selectedUsers: Set<String>,
    onUserSelectionChange: (String, Boolean) -> Unit,
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
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text("이름 또는 이메일로 검색") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
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
                        searchQuery.isNotBlank() && searchResults.isEmpty() && !isLoading -> {
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
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(user.profileImageUrl ?: R.drawable.ic_account_circle_24)
                .error(R.drawable.ic_account_circle_24)
                .placeholder(R.drawable.ic_account_circle_24)
                .build(),
            contentDescription = "${user.userName} 프로필",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = user.userName,
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
        AddMemberDialog(
            onDismissRequest = {},
            onAddMembers = {},
            searchQuery = "김",
            onSearchQueryChange = {},
            searchResults = listOf(
                UserSearchResult(
                    userId = "user1",
                    userName = "김영희",
                    userEmail = "kim@example.com",
                    profileImageUrl = null
                ),
                UserSearchResult(
                    userId = "user2",
                    userName = "김철수",
                    userEmail = "kim2@example.com",
                    profileImageUrl = null
                )
            ),
            selectedUsers = setOf("user1"),
            onUserSelectionChange = { _, _ -> },
            isLoading = false,
            error = null
        )
    }
}

/**
 * 미리보기: 로딩 중인 멤버 추가 다이얼로그
 */
@Preview(showBackground = true)
@Composable
fun AddMemberDialogLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AddMemberDialog(
            onDismissRequest = {},
            onAddMembers = {},
            searchQuery = "김",
            onSearchQueryChange = {},
            searchResults = emptyList(),
            selectedUsers = emptySet(),
            onUserSelectionChange = { _, _ -> },
            isLoading = true,
            error = null
        )
    }
}

/**
 * 미리보기: 오류가 발생한 멤버 추가 다이얼로그
 */
@Preview(showBackground = true)
@Composable
fun AddMemberDialogErrorPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AddMemberDialog(
            onDismissRequest = {},
            onAddMembers = {},
            searchQuery = "김",
            onSearchQueryChange = {},
            searchResults = emptyList(),
            selectedUsers = emptySet(),
            onUserSelectionChange = { _, _ -> },
            isLoading = false,
            error = "사용자 검색 중 오류가 발생했습니다."
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
@Composable
fun AddMemberDialog(
    projectId: String,
    onDismissRequest: () -> Unit,
    onMemberAdded: () -> Unit
) {
    // 실제 구현에서는 ViewModel을 사용하여 검색 및 추가 기능 구현
    // 여기서는 간단한 구현으로 대체

    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember { mutableStateListOf<UserSearchResult>() }
    val selectedUsers = remember { mutableStateSetOf<String>() }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // 검색어 변경 핸들러
    val onSearchQueryChange = { query: String ->
        searchQuery = query
        // 실제로는 ViewModel에서 검색 기능 구현
        if (query.length >= 2) {
            isLoading = true
            error = null
            
            // 더미 데이터로 대체 (실제로는 API 호출)
            searchResults.clear()
            searchResults.addAll(listOf(
                UserSearchResult("user1", "김영희", "kim@example.com", null),
                UserSearchResult("user2", "이철수", "lee@example.com", null)
            ))
            isLoading = false
        } else {
            searchResults.clear()
        }
    }

    // 멤버 추가 핸들러
    val onAddMembers = { selected: Set<String> ->
        // 실제로는 ViewModel을 통해 서버 API 호출
        println("Adding members to project $projectId: $selected")
        onMemberAdded()
    }

    // 사용자 선택 핸들러를 래핑하여 Boolean 대신 Unit을 반환하는 새 핸들러 생성
    val wrappedOnUserSelectionChange: (String, Boolean) -> Unit = { userId, isSelected ->
        if (isSelected) {
            selectedUsers.add(userId)
        } else {
            selectedUsers.remove(userId)
        }
    }

    AddMemberDialog(
        onDismissRequest = onDismissRequest,
        onAddMembers = onAddMembers,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        searchResults = searchResults,
        selectedUsers = selectedUsers,
        onUserSelectionChange = wrappedOnUserSelectionChange,
        isLoading = isLoading,
        error = error
    )
} 