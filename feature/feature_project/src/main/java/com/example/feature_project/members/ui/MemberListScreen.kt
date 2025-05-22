package com.example.feature_project.members.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd // 멤버 추가 아이콘
import androidx.compose.material.icons.filled.Search // ★ 검색 아이콘 import 추가
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core_common.util.DateTimeUtil
import com.example.core_navigation.core.AppNavigator
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.core.NavigationCommand
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.core_ui.R
import com.example.feature_project.members.viewmodel.MemberListEvent
import com.example.feature_project.members.viewmodel.MemberListUiState
import com.example.feature_project.members.viewmodel.MemberListViewModel
import com.example.domain.model.ProjectMember // Import domain model for dialog
import kotlinx.coroutines.flow.collectLatest

/**
 * MemberListScreen: 프로젝트 멤버 목록 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberListScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: MemberListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmationDialog by remember { mutableStateOf<ProjectMember?>(null) }
    var showAddMemberDialogState by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is MemberListEvent.NavigateToEditMember -> {
                    appNavigator.navigate(
                        NavigationCommand.NavigateToRoute.fromRoute(
                            AppRoutes.Project.editMember(event.projectId, event.userId)
                        )
                    )
                }
                is MemberListEvent.ShowDeleteConfirm -> {
                    showDeleteConfirmationDialog = event.member
                }
                is MemberListEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is MemberListEvent.ShowAddMemberDialog -> {
                    showAddMemberDialogState = true
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("멤버 관리") },
                navigationIcon = {
                    IconButton(onClick = { appNavigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onAddMemberClick() }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "멤버 초대")
                    }
                }
            )
        }
    ) { paddingValues ->
        MemberListContent(
            paddingValues = paddingValues,
            uiState = uiState,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onMemberClick = viewModel::onMemberClick,
            onDeleteMemberClick = viewModel::requestDeleteMember
        )
    }

    // 멤버 삭제 확인 다이얼로그
    showDeleteConfirmationDialog?.let { member ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = null },
            title = { Text("멤버 내보내기") },
            text = { Text("${member.userName}님을 프로젝트에서 내보내시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDeleteMember(member)
                        showDeleteConfirmationDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("내보내기") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = null }) { Text("취소") }
            }
        )
    }

    // 멤버 추가 다이얼로그
    if (showAddMemberDialogState) {
        AddMemberDialog(
            projectId = uiState.projectId,
            onDismissRequest = { showAddMemberDialogState = false },
            onMemberAdded = {
                showAddMemberDialogState = false
                viewModel.refreshMembers()
            }
        )
    }
}

/**
 * MemberListContent: 멤버 목록 UI (Stateless)
 */
@Composable
fun MemberListContent(
    paddingValues: PaddingValues,
    uiState: MemberListUiState, // This uiState now contains List<ProjectMember>
    onSearchQueryChanged: (String) -> Unit,
    onMemberClick: (ProjectMember) -> Unit, // Changed
    onDeleteMemberClick: (ProjectMember) -> Unit, // Changed
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 검색 바 (선택 사항)
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                label = { Text("멤버 검색") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
        }

        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (uiState.error != null) {
            item {
                Text(
                    text = "오류: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else if (uiState.members.isEmpty()) {
             item {
                Text(
                    text = "프로젝트 멤버가 없습니다.",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(uiState.members, key = { it.userId }) { member ->
                ProjectMemberListItemComposable(
                    member = member, // Pass ProjectMember directly
                    onClick = { onMemberClick(member) },
                    onMoreClick = { onDeleteMemberClick(member) }
                )
            }
        }
    }
}

/**
 * ProjectMemberListItemComposable: 개별 프로젝트 멤버 아이템 UI (Stateless)
 */
@Composable
fun ProjectMemberListItemComposable(
    member: ProjectMember, // Changed parameter to ProjectMember
    onClick: (ProjectMember) -> Unit, // Changed
    onMoreClick: (ProjectMember) -> Unit, // Changed
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(member) } // Use member
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(member.profileImageUrl)
                .error(R.drawable.ic_account_circle_24)
                .placeholder(R.drawable.ic_account_circle_24)
                .build(),
            contentDescription = "${member.userName}님의 프로필 사진",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.userName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (member.roleIds.isNotEmpty()) { // Changed from member.roles to member.roleIds
                Text(
                    text = "역할 ID: " + member.roleIds.joinToString(), // Display role IDs
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = { onMoreClick(member) }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "더 보기")
        }
    }
}

// --- Preview ---
// Preview might need adjustment if ProjectMemberUiItem is not directly constructible here
// or if the dummy data for ProjectMember in MemberListUiState is not compatible.
// For now, keeping the preview as is, but it might show errors or require updates
// to use ProjectMemberUiItem for the 'members' list in the preview UiState.
@Preview(showBackground = true)
@Composable
private fun MemberListContentPreview() {
    // Preview용 Role 객체 생성
    val previewRoleAdmin = com.example.domain.model.Role(id = "r_admin", projectId = "p_preview", name = "관리자", permissions = listOf(com.example.domain.model.RolePermission.MANAGE_MEMBERS), memberCount = 1)
    val previewRoleMember = com.example.domain.model.Role(id = "r_member", projectId = "p_preview", name = "팀원", permissions = listOf(com.example.domain.model.RolePermission.READ_MESSAGES), memberCount = 3)
    val previewRoleSupporter = com.example.domain.model.Role(id = "r_supporter", projectId = "p_preview", name = "서포터", permissions = emptyList(), memberCount = 2)
    val previewRoleViewer = com.example.domain.model.Role(id = "r_viewer", projectId = "p_preview", name = "뷰어", permissions = emptyList(), memberCount = 5)

    val previewMembers = listOf(
        ProjectMember("u1", "Alice Wonderland", "url_to_image_1", listOf("r_admin"), DateTimeUtil.nowInstant()),
        ProjectMember("u2", "Bob The Builder", null, listOf("r_admin", "r_member"), DateTimeUtil.nowInstant()),
        ProjectMember("u3", "Charlie Brown", "url_to_image_3", listOf("r_member", "r_supporter"), DateTimeUtil.nowInstant()),
        ProjectMember("u4", "Diana Prince", null, listOf("r_viewer"), DateTimeUtil.nowInstant())
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            MemberListContent(
                paddingValues = PaddingValues(),
                uiState = MemberListUiState(
                    isLoading = false,
                    error = null,
                    members = previewMembers, // Use ProjectMember list directly
                    searchQuery = ""
                ),
                onSearchQueryChanged = {},
                onMemberClick = {},
                onDeleteMemberClick = {}
            )
        }
    }
}