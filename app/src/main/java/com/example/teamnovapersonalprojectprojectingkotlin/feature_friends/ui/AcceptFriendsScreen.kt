package com.example.teamnovapersonalprojectprojectingkotlin.feature_friends.ui

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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.teamnovapersonalprojectprojectingkotlin.R // 기본 이미지 리소스 (경로 확인 필요)
// ViewModel 및 관련 상태/이벤트/UI 모델 Import
import com.example.teamnovapersonalprojectprojectingkotlin.feature_friends.viewmodel.AcceptFriendsEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_friends.viewmodel.AcceptFriendsUiState
import com.example.teamnovapersonalprojectprojectingkotlin.feature_friends.viewmodel.AcceptFriendsViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.feature_friends.viewmodel.FriendRequestItem // ★ UI 모델 Import
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * AcceptFriendsScreen: 받은 친구 요청 목록을 보고 수락/거절하는 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptFriendsScreen(
    modifier: Modifier = Modifier,
    viewModel: AcceptFriendsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리 (스낵바 등)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AcceptFriendsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                // NavigateBack 이벤트는 Screen에서 처리하지 않고, 필요 시 ViewModel에서 직접 호출 가능
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("친구 요청 수락하기") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { // 네비게이션 콜백 사용
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
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
            // ★ friendRequests 타입은 List<FriendRequestItem>
            uiState.friendRequests.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("받은 친구 요청이 없습니다.")
                }
            }
            else -> {
                AcceptFriendsListContent(
                    modifier = Modifier.padding(paddingValues),
                    requests = uiState.friendRequests, // ★ UI 모델 리스트 전달
                    onAcceptClick = viewModel::acceptFriendRequest, // ViewModel 함수 호출
                    onDenyClick = viewModel::denyFriendRequest // ViewModel 함수 호출
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
    requests: List<FriendRequestItem>, // ★ UI 모델 타입 사용
    onAcceptClick: (String) -> Unit, // userId 전달
    onDenyClick: (String) -> Unit // userId 전달
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp) // 목록 상하 패딩
    ) {
        items(
            items = requests,
            key = { it.userId } // ★ userId를 Key로 사용
        ) { request ->
            FriendRequestItemComposable( // ★ Composable 이름 변경 (다른 파일과 충돌 방지)
                request = request, // ★ UI 모델 전달
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
fun FriendRequestItemComposable( // ★ Composable 이름 변경
    request: FriendRequestItem, // ★ UI 모델 타입 사용
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
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(request.profileImageUrl ?: R.drawable.ic_account_circle_24) // 기본 이미지 사용
                .error(R.drawable.ic_account_circle_24)
                .placeholder(R.drawable.ic_account_circle_24)
                .build(),
            contentDescription = "${request.userName} 프로필",
            modifier = Modifier.size(48.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = request.userName, // ★ UI 모델 필드 사용
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
private fun AcceptFriendsListContentPreview() {
    // Preview용 가짜 데이터 (UI 모델 사용)
    val previewRequests = listOf(
        FriendRequestItem("u10", "요청자1", null),
        FriendRequestItem("u11", "요청자2", "url..."),
        FriendRequestItem("u12", "요청자3", null)
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("친구 요청 수락하기") }) }
        ) { padding ->
            AcceptFriendsListContent(
                modifier = Modifier.padding(padding),
                requests = previewRequests, // ★ UI 모델 리스트 사용
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