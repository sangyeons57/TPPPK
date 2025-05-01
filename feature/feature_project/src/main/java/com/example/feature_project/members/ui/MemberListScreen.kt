package com.example.teamnovapersonalprojectprojectingkotlin.feature_project_members.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd // 멤버 추가 아이콘
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.teamnovapersonalprojectprojectingkotlin.R // 기본 이미지 리소스
// ViewModel 및 관련 요소 Import
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project_members.viewmodel.MemberListEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project_members.viewmodel.MemberListViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project_members.viewmodel.ProjectMemberItem // ★ UI 모델 Import
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * MemberListScreen: 프로젝트 멤버 목록 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberListScreen(
    modifier: Modifier = Modifier,
    viewModel: MemberListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToEditMember: (projectId: String, userId: String) -> Unit,
    onShowAddMemberDialog: (projectId: String) -> Unit // 멤버 추가 다이얼로그 표시 콜백
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리 (스낵바, 네비게이션 등)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is MemberListEvent.NavigateToEditMember -> onNavigateToEditMember(event.projectId, event.userId)
                is MemberListEvent.ShowAddMemberDialog -> onShowAddMemberDialog(event.projectId)
                is MemberListEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("프로젝트 멤버") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onAddMemberClick) { // 멤버 추가 액션
                        Icon(Icons.Filled.PersonAdd, contentDescription = "멤버 추가")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 로딩 및 에러 상태 처리
        when {
            uiState.isLoading && uiState.members.isEmpty() -> { // 초기 로딩
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            }
            uiState.members.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("프로젝트 멤버가 없습니다.")
                }
            }
            else -> {
                // 멤버 목록 표시
                MemberListContent(
                    modifier = Modifier.padding(paddingValues),
                    members = uiState.members, // ★ UI 모델 리스트 전달
                    onMemberClick = viewModel::onMemberClick // ViewModel 함수 호출 (userId 전달)
                )
            }
        }
        // 백그라운드 로딩 인디케이터 (새로고침 시)
        if (uiState.isLoading && uiState.members.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 56.dp), contentAlignment = Alignment.TopCenter) { // TopAppBar 아래 중앙
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) // 또는 CircularProgressIndicator
            }
        }
    }
}

/**
 * MemberListContent: 멤버 목록 UI (Stateless)
 */
@Composable
fun MemberListContent(
    modifier: Modifier = Modifier,
    members: List<ProjectMemberItem>, // ★ UI 모델 타입 사용
    onMemberClick: (String) -> Unit // userId 전달
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = members,
            key = { it.userId } // ★ userId를 Key로 사용
        ) { member ->
            ProjectMemberListItemComposable( // ★ Composable 이름 변경
                member = member, // ★ UI 모델 전달
                onClick = { onMemberClick(member.userId) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 88.dp)) // 프로필 이미지 영역 이후부터 Divider
        }
    }
}

/**
 * ProjectMemberListItemComposable: 개별 프로젝트 멤버 아이템 UI (Stateless)
 */
@Composable
fun ProjectMemberListItemComposable( // ★ Composable 이름 변경
    member: ProjectMemberItem, // ★ UI 모델 타입 사용
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // 클릭 시 멤버 편집 화면 이동
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(member.profileImageUrl ?: R.drawable.ic_account_circle_24) // 기본 이미지
                .error(R.drawable.ic_account_circle_24)
                .placeholder(R.drawable.ic_account_circle_24)
                .build(),
            contentDescription = "${member.userName} 프로필",
            modifier = Modifier.size(48.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.userName, // ★ UI 모델 필드 사용
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (member.rolesText.isNotEmpty()) { // 역할이 있을 경우 표시
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = member.rolesText, // ★ UI 모델 필드 사용 (포맷된 역할 문자열)
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // TODO: 필요시 추가 액션 버튼 (예: 추방 - 권한 확인 필요)
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun MemberListContentPreview() {
    val previewMembers = listOf(
        ProjectMemberItem("u1", "멤버1 (관리자)", null, "관리자"),
        ProjectMemberItem("u2", "멤버2 멤버2 멤버2 멤버2", "url...", "팀원"),
        ProjectMemberItem("u3", "멤버3", null, "뷰어, 게스트"),
        ProjectMemberItem("u4", "멤버4", null, "") // 역할 없음
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            MemberListContent(
                members = previewMembers,
                onMemberClick = {}
            )
        }
    }
}