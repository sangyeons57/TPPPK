package com.example.feature_friends.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check // 수락 아이콘
import androidx.compose.material.icons.filled.Close // 거절 아이콘
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
// Removed direct Coil imports, will use UserProfileImage
import com.example.core_navigation.core.AppNavigator
import com.example.core_ui.components.user.UserProfileImage // Import the new composable
import com.example.feature_friends.viewmodel.AcceptFriendsEvent
import com.example.feature_friends.viewmodel.AcceptFriendsViewModel
import com.example.feature_friends.viewmodel.FriendRequestItem
// ViewModel 및 관련 상태/이벤트/UI 모델 Import
import kotlinx.coroutines.flow.collectLatest
import com.example.core_ui.R
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme

/**
 * AcceptFriendsScreen: 받은 친구 요청 목록을 보고 수락/거절하는 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptFriendsScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: AcceptFriendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리 (스낵바 등)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AcceptFriendsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is AcceptFriendsEvent.NavigateBack -> appNavigator.navigateBack()
                // NavigateBack 이벤트는 Screen에서 처리하지 않고, 필요 시 ViewModel에서 직접 호출 가능
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("친구 요청") },
                navigationIcon = {
                    DebouncedBackButton(onClick = { appNavigator.navigateBack() })
                }
            )
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
            uiState.friendRequests.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("받은 친구 요청이 없습니다.")
                }
            }
            else -> {
                AcceptFriendsListContent(
                    modifier = Modifier.padding(paddingValues),
                    requests = uiState.friendRequests,
                    onAcceptClick = viewModel::acceptFriendRequest,
                    onDenyClick = viewModel::denyFriendRequest
                )
            }
        }
    }
}

/**
 * AcceptFriendsListContent: 친구 요청 목록 UI (Stateless)
 */
@Composable
fun AcceptFriendsListContent(
    modifier: Modifier = Modifier,
    requests: List<FriendRequestItem>,
    onAcceptClick: (String) -> Unit,
    onDenyClick: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp) // 목록 상하 패딩
    ) {
        items(
            items = requests,
            key = { it.userId }
        ) { request ->
            FriendRequestItemComposable(
                request = request,
                onAcceptClick = { onAcceptClick(request.userId) },
                onDenyClick = { onDenyClick(request.userId) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

/**
 * FriendRequestItemComposable: 개별 친구 요청 아이템 UI (Stateless)
 */
@Composable
fun FriendRequestItemComposable(
    request: FriendRequestItem,
    onAcceptClick: () -> Unit,
    onDenyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp), // 좌우, 상하 패딩
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserProfileImage(
            profileImageUrl = request.profileImageUrl,
            contentDescription = "${request.userName} 프로필",
            modifier = Modifier.size(48.dp).clip(CircleShape)
            // contentScale is handled by UserProfileImage default or can be passed if needed
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = request.userName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f) // 남은 공간 차지
        )
        // 수락 버튼
        IconButton(
            onClick = onAcceptClick,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.Check, contentDescription = "수락")
        }
        Spacer(modifier = Modifier.width(8.dp))
        // 거절 버튼
        IconButton(
            onClick = onDenyClick,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "거절")
        }
    }
}


// --- Preview ---
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun AcceptFriendsScreenPreview() {
    // 미리보기용 가짜 데이터 생성
    val sampleRequests = listOf(
        FriendRequestItem("user1", "사용자1", null),
        FriendRequestItem("user2", "사용자2", null)
    )
    
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("친구 요청 수락하기") }) }
        ) { padding ->
            AcceptFriendsListContent(
                modifier = Modifier.padding(padding),
                requests = sampleRequests, // previewRequests -> sampleRequests
                onAcceptClick = {},
                onDenyClick = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Accept Friends Empty")
@Composable
private fun AcceptFriendsListEmptyPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("친구 요청 수락하기") }) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("받은 친구 요청이 없습니다.")
            }
        }
    }
}