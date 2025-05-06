package com.example.feature_friends.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.core.NavigationCommand
import com.example.core_ui.R
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_friends.viewmodel.FriendItem
import com.example.feature_friends.viewmodel.FriendViewModel
import com.example.feature_friends.viewmodel.FriendsEvent
import kotlinx.coroutines.flow.collectLatest
import java.util.Date // Preview용 Date 임포트

/**
 * FriendsScreen: 친구 목록 표시 및 관리 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    navigationManager: ComposeNavigationHandler,
    modifier: Modifier = Modifier,
    viewModel: FriendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is FriendsEvent.NavigateToAcceptFriends -> navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Friends.ACCEPT_REQUESTS))
                is FriendsEvent.NavigateToChat -> navigationManager.navigate(NavigationCommand.NavigateToRoute(AppRoutes.Chat.chat(event.channelId)))
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
                    IconButton(onClick = { navigationManager.navigateBack() }) {
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
                Text("친구 요청 수락하기")
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
                    onFriendClick = viewModel::onFriendClick // ViewModel 함수 호출
                )
            }
        }
    }

    // 친구 추가 다이얼로그
    if (uiState.showAddFriendDialog) {
        AddFriendDialog(
            onDismissRequest = viewModel::requestAddFriendToggle
        )
    }
}

/**
 * FriendsListContent: 친구 목록 UI (Stateless)
 */
@Composable
fun FriendsListContent(
    modifier: Modifier = Modifier,
    friends: List<FriendItem>,
    onFriendClick: (String) -> Unit // friendId 전달
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
                onClick = { onFriendClick(friend.friendId) } // 변경
            )
            HorizontalDivider() // 아이템 사이에 구분선 추가
        }
    }
}

/**
 * FriendListItem: 개별 친구 아이템 UI (Stateless)
 */
@Composable
fun FriendListItem(
    friend: FriendItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp), // 아이템 상하 패딩
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.ic_account_circle_24) // 기본 이미지 사용
                .error(R.drawable.ic_account_circle_24)
                .placeholder(R.drawable.ic_account_circle_24)
                .build(),
            contentDescription = "${friend.displayName} 프로필", // 변경
            modifier = Modifier.size(48.dp).clip(CircleShape), // 크기 조절
            contentScale = ContentScale.Crop
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
        // TODO: DM 바로가기 버튼 또는 다른 액션 버튼 추가 가능 (옵션)
        // TextButton(onClick = onClick) { Text("DM") }
    }
}


// --- Preview ---
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun FriendsListContentPreview() {
    val previewFriends = listOf(
        FriendItem("u1", "accepted", Date(), null, "Friend: u1"),
        FriendItem("u2", "pending_sent", Date(), null, "Friend: u2"),
        FriendItem("u3", "accepted", Date(), Date(), "Friend: u3")
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("친구") }) },
            bottomBar = { Button(onClick = {}, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("친구 요청 수락하기") } }
        ) { padding ->
            FriendsListContent(
                modifier = Modifier.padding(padding),
                friends = previewFriends,
                onFriendClick = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Friends List Empty")
@Composable
private fun FriendsListEmptyPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("친구") }) },
            bottomBar = { Button(onClick = {}, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("친구 요청 수락하기") } }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("친구가 없습니다. 친구를 추가해보세요!")
            }
        }
    }
}
