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
import androidx.compose.ui.window.Dialog // 기본 Dialog 사용
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.core_ui.R

// --- 데이터 모델 (호출하는 쪽에서 정의하고 전달해야 함) ---
// 이 파일 내에 둘 수도 있지만, 보통 ViewModel과 공유하므로 별도 파일이나 ViewModel 내부에 정의
data class UserSearchResult(
    val userId: String,
    val userName: String,
    val userEmail: String?, // 또는 다른 식별자
    val profileImageUrl: String?
)


/**
 * AddMemberDialog: 프로젝트에 멤버를 검색하고 추가하는 다이얼로그 Composable
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
    onAddMembers: (Set<String>) -> Unit, // 선택된 사용자 ID Set 전달
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<UserSearchResult>,
    selectedUsers: Set<String>, // userId Set
    onUserSelectionChange: (String, Boolean) -> Unit, // userId, isSelected
    isLoading: Boolean,
    error: String?
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Dialog Composable 사용
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.large, // 다이얼로그 모양
            modifier = Modifier
                .fillMaxWidth()
                // 높이 제한 (화면 크기에 따라 조절 가능)
                .heightIn(min = 200.dp, max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("멤버 초대", style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(16.dp))

                // 사용자 검색 필드
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
                            IconButton(onClick = { onSearchQueryChange("") }) { // 검색어 지우기
                                Icon(Icons.Filled.Clear, contentDescription = "지우기")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus() // 검색 실행 시 키보드 숨김
                        // TODO: ViewModel에서 검색 실행 로직 호출 (현재는 onChange에서 debounce로 처리 가정)
                    })
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 검색 결과 목록 영역
                Box(modifier = Modifier.weight(1f)) { // 남은 공간 채우기
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
                            // 검색 결과 목록 (스크롤 가능)
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

                // 하단 버튼 영역
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End // 버튼 오른쪽 정렬
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAddMembers(selectedUsers) },
                        enabled = selectedUsers.isNotEmpty() // 선택된 사용자가 있을 때만 활성화
                    ) {
                        Text("추가")
                    }
                }
            }
        }
    }

    // 다이얼로그가 처음 표시될 때 검색 필드에 포커스
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * UserSearchResultItem: 사용자 검색 결과 목록의 각 항목 UI
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
            .clickable { onCheckedChange(!isSelected) } // 행 클릭으로 선택/해제
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
            modifier = Modifier.size(40.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.userName, style = MaterialTheme.typography.bodyLarge)
            if (user.userEmail != null) {
                Text(
                    user.userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = null // Row 클릭으로 처리
        )
    }
}


// --- Preview ---
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AddMemberDialogPreview_Results() {
    val searchResults = remember {
        mutableStateListOf(
            UserSearchResult("u1", "김철수", "chul@example.com", null),
            UserSearchResult("u2", "이영희", "young@example.com", "url..."),
            UserSearchResult("u3", "박민준", "min@example.com", null)
        )
    }
    val selectedUsers = remember { mutableStateOf(setOf("u2")) }

    TeamnovaPersonalProjectProjectingKotlinTheme {
        // 다이얼로그 프리뷰를 위해 Box 사용
        Box(modifier = Modifier.fillMaxSize()) {
            AddMemberDialog(
                onDismissRequest = { },
                onAddMembers = {},
                searchQuery = "김",
                onSearchQueryChange = {},
                searchResults = searchResults,
                selectedUsers = selectedUsers.value,
                onUserSelectionChange = { userId, isSelected ->
                    if (isSelected) selectedUsers.value += userId
                    else selectedUsers.value -= userId
                },
                isLoading = false,
                error = null
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AddMemberDialogPreview_Loading() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AddMemberDialog(
                onDismissRequest = { },
                onAddMembers = {},
                searchQuery = "박",
                onSearchQueryChange = {},
                searchResults = emptyList(),
                selectedUsers = emptySet(),
                onUserSelectionChange = { _, _ -> },
                isLoading = true,
                error = null
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AddMemberDialogPreview_Empty() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AddMemberDialog(
                onDismissRequest = { },
                onAddMembers = {},
                searchQuery = "없는사용자",
                onSearchQueryChange = {},
                searchResults = emptyList(),
                selectedUsers = emptySet(),
                onUserSelectionChange = { _, _ -> },
                isLoading = false,
                error = null
            )
        }
    }
}