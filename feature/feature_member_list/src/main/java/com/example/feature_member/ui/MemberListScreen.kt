package com.example.feature_member.ui

// Removed direct Coil imports
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.components.user.UserProfileImage
import com.example.domain.model.ui.data.MemberUiModel
import com.example.feature_member.dialog.ui.AddMemberDialog
import com.example.feature_member.viewmodel.MemberListEvent
import com.example.feature_member.viewmodel.MemberListUiState
import com.example.feature_member.viewmodel.MemberListViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * MemberListScreen: 프로젝트 멤버 목록 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberListScreen(
    navigationManger: NavigationManger,
    modifier: Modifier = Modifier,
    viewModel: MemberListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmationDialog by remember { mutableStateOf<MemberUiModel?>(null) } // Changed type
    var showAddMemberDialogState by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
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
                    IconButton(onClick = { navigationManger.navigateBack() }) {
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
    showDeleteConfirmationDialog?.let { memberUiModel -> // Changed variable name for clarity
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = null },
            title = { Text("멤버 내보내기") },
            text = { Text("${memberUiModel.userName}님을 프로젝트에서 내보내시겠습니까?") }, // Used MemberUiModel.userName
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDeleteMember(memberUiModel) // Pass MemberUiModel
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
    uiState: MemberListUiState, // This uiState now contains List<MemberUiModel>
    onSearchQueryChanged: (String) -> Unit,
    onMemberClick: (MemberUiModel) -> Unit, // Changed
    onDeleteMemberClick: (MemberUiModel) -> Unit, // Changed
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                label = { Text("멤버 검색") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
        }

        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), contentAlignment = Alignment.Center) {
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
            items(uiState.members, key = { it.userId.value }) { member ->
                ProjectMemberListItemComposable(
                    member = member, // Pass Member directly
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
    member: MemberUiModel, // Changed parameter to MemberUiModel
    onClick: (MemberUiModel) -> Unit, // Changed
    onMoreClick: (MemberUiModel) -> Unit, // Changed
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(member) } // Use member (which is MemberUiModel)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserProfileImage(
            profileImageUrl = member.profileImageUrl?.value, // Use MemberUiModel.profileImageUrl
            contentDescription = "${member.userName}님의 프로필 사진", // Use MemberUiModel.userName
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.userName.value, // Use MemberUiModel.userName
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (member.roleNames.isNotEmpty()) { // Use MemberUiModel.roleNames
                Text(
                    text = member.roleNames.joinToString(", ") { it.value }, // ValueObject에서 .value 사용
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = { onMoreClick(member) }) { // Pass MemberUiModel
            Icon(Icons.Filled.MoreVert, contentDescription = "더 보기")
        }
    }
}

@Preview
@Composable 
fun MemberListScreenPreview() {
    MemberListScreen(
        navigationManger = TODO(),
        modifier = TODO(),
        viewModel = TODO()
    )
}