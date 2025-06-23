package com.example.feature_friends.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward // Icon for DM button
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// Removed direct Coil imports
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.components.user.UserProfileImage // Import the new composable
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.core.NavigationCommand
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_friends.viewmodel.FriendItem
import com.example.feature_friends.viewmodel.FriendViewModel
import com.example.feature_friends.viewmodel.FriendsEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch // Required for launching coroutines

/**
 * FriendsScreen: 친구 목록 표시 및 관리 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    navigationManger: NavigationManger,
    modifier: Modifier = Modifier,
    viewModel: FriendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope() // Scope for launching coroutines

    // State for ModalBottomSheet
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // Ensures the sheet is fully expanded or hidden
    )

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is FriendsEvent.NavigateToAcceptFriends -> navigationManger.navigate(
                    NavigationCommand.NavigateToRoute.fromRoute(AppRoutes.Friends.ACCEPT_REQUESTS)
                )
                is FriendsEvent.NavigateToChat -> navigationManger.navigate(
                    NavigationCommand.NavigateToRoute.fromRoute(AppRoutes.Chat.screen(event.channelId))
                )
                is FriendsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },

                topBar = {
                    TopAppBar(
                        title = { Text("친구") },
                        navigationIcon = {
                            IconButton(onClick = { navigationManger.navigateBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                            }
                        },
                        actions = {
                            // 친구 추가하기 텍스트 버튼 (기존 XML의 TextView 역할)
                            TextButton(onClick = viewModel::requestAddFriendToggle) {
                                Text("친구 추가하기")
                            }
                        }
                    )
                },
                bottomBar = {
                    // 친구 요청 수락하기 버튼 (기존 XML의 Button 역할)
                    Button(
                        onClick = viewModel::onAcceptFriendClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding() // 하단 네비게이션 바 패딩 적용
                    ) {
                        Text("친구 요청")
                    }
                }
            ) { paddingValues ->
                // 로딩 및 에러 상태 처리
                when {
                    uiState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.error != null -> {
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                            Text("오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    uiState.friends.isEmpty() && !uiState.isLoading -> { // 로딩 중 아닐 때만 빈 상태 표시
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                            Text("친구가 없습니다. 친구를 추가해보세요!")
                        }
                    }
                    else -> {
                        FriendsListContent(
                            modifier = Modifier.padding(paddingValues),
                            friends = uiState.friends,
                            onItemClick = { friendId ->
                                scope.launch {
                                    showBottomSheet = true
                                    sheetState.show() // Show the bottom sheet
                                }
                            },
                            onDmChannelClick = viewModel::onFriendClick // Navigate to DM
                        )
                    }
                }
    }

    // Conditionally display ModalBottomSheet (M3)
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            // Actual content for the bottom sheet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Bottom Sheet Dialog Content")
            }
        }
    }

    // AddFriendDialog
    if (uiState.showAddFriendDialog) {
        AddFriendDialog(onDismissRequest = viewModel::requestAddFriendToggle)
    }


}

/**
 * FriendsListContent: 친구 목록 UI (Stateless)
 */
@Composable
fun FriendsListContent(
    modifier: Modifier = Modifier,
    friends: List<FriendItem>,
    onItemClick: (String) -> Unit,    // For item click (show bottom sheet)
    onDmChannelClick: (String) -> Unit // For DM button
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp), // 좌우 패딩 추가
        contentPadding = PaddingValues(vertical = 8.dp) // 목록 상하 패딩
    ) {
        items(
            items = friends,
            key = { it.friendId } // 변경
        ) { friend ->
            FriendListItem(
                friend = friend,
                onItemClick = { onItemClick(friend.friendId) },
                onDmChannelClick = { onDmChannelClick(friend.friendId) }
            )
            HorizontalDivider()
        }
    }
}

/**
 * FriendListItem: 개별 친구 아이템 UI (Stateless)
 */
@Composable
fun FriendListItem(
    friend: FriendItem,
    onItemClick: () -> Unit,  // Click for the whole item
    onDmChannelClick: () -> Unit, // Click for DM button
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick) // Whole item click
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserProfileImage(
            profileImageUrl = friend.profileImageUrl, // Use the actual profile image URL
            contentDescription = "${friend.displayName} 프로필",
            modifier = Modifier.size(48.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.displayName, // 변경
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Status: ${friend.status}", // "Status: " 접두사 추가
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            // 예시: friend.relationshipTimestamp.toString() 등으로 시간 표시 가능
        }
        // DM 채널 바로가기 버튼
        IconButton(onClick = onDmChannelClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward, // "입장" 아이콘
                contentDescription = "DM ${friend.displayName}"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Friends List Empty")
@Composable
private fun FriendsListEmptyPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold (
            topBar = { TopAppBar(title = { Text("친구") }) },
            bottomBar = { Button(onClick = {}, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("친구 요청 수락하기") } }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("친구가 없습니다. 친구를 추가해보세요!")
            }
        }
    }
}
