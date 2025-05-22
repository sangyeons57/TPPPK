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
import com.example.feature_project.members.viewmodel.ProjectMemberUiItem // Added import
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
    uiState: MemberListUiState, // This uiState now contains List<ProjectMemberUiItem>
    onSearchQueryChanged: (String) -> Unit,
    onMemberClick: (ProjectMemberUiItem) -> Unit, // Changed
    onDeleteMemberClick: (ProjectMemberUiItem) -> Unit, // Changed
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
            items(uiState.members, key = { it.userId }) { memberUiItem -> // memberUiItem is ProjectMemberUiItem
                ProjectMemberListItemComposable(
                    memberUiItem = memberUiItem, // Pass the UiItem
                    onClick = { item -> onMemberClick(item) }, // Pass ProjectMemberUiItem
                    onMoreClick = { item -> onDeleteMemberClick(item) } // Pass ProjectMemberUiItem
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
    memberUiItem: ProjectMemberUiItem, // Changed parameter
    onClick: (ProjectMemberUiItem) -> Unit, // Changed
    onMoreClick: (ProjectMemberUiItem) -> Unit, // Changed
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(memberUiItem) } // Use memberUiItem
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(memberUiItem.profileImageUrl ?: R.drawable.ic_account_circle_24) // Use memberUiItem
                .error(R.drawable.ic_account_circle_24)
                .placeholder(R.drawable.ic_account_circle_24)
                .build(),
            contentDescription = "${memberUiItem.userName} 프로필", // Use memberUiItem
            modifier = Modifier.size(48.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = memberUiItem.userName, // Use memberUiItem
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Display rolesText from memberUiItem
            if (memberUiItem.rolesText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = memberUiItem.rolesText, // Use memberUiItem.rolesText
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = { onMoreClick(memberUiItem) }) { // Use memberUiItem
             Icon(Icons.Default.MoreVert, contentDescription = "더보기")
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
    // This preview will likely break because MemberListUiState expects List<ProjectMemberUiItem>
    // and ProjectMemberListItemComposable expects ProjectMemberUiItem.
    // To fix, create ProjectMemberUiItem instances for previewMembers.
    val previewMemberItems = listOf(
        ProjectMemberUiItem("u1", "멤버1", null, "관리자, 팀원", java.time.Instant.now(), ProjectMember("u1", "멤버1", null, listOf("role1", "role2"), java.time.Instant.now())),
        ProjectMemberUiItem("u2", "멤버2 멤버2", "url...", "팀원", java.time.Instant.now(), ProjectMember("u2", "멤버2 멤버2", "url...", listOf("role2"), java.time.Instant.now())),
        ProjectMemberUiItem("u3", "멤버3", null, "뷰어", java.time.Instant.now(), ProjectMember("u3", "멤버3", null, listOf("role3"), java.time.Instant.now())),
        ProjectMemberUiItem("u4", "멤버4", null, "역할 없음", java.time.Instant.now(), ProjectMember("u4", "멤버4", null, emptyList(), java.time.Instant.now()))
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            MemberListContent(
                paddingValues = PaddingValues(),
                uiState = MemberListUiState(
                    isLoading = false,
                    error = null,
                    members = previewMemberItems, // Use ProjectMemberUiItem list
                    searchQuery = ""
                ),
                onSearchQueryChanged = {},
                onMemberClick = {},
                onDeleteMemberClick = {}
            )
        }
    }
}