package com.example.feature_member_list.ui

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.components.user.UserProfileImage
import com.example.domain.model.ui.data.MemberUiModel
import com.example.domain.vo.UserId
import com.example.feature_member_list.dialog.ui.AddMemberDialog
import com.example.feature_member_list.viewmodel.MemberListEvent
import com.example.feature_member_list.viewmodel.MemberListUiState
import com.example.feature_member_list.viewmodel.MemberListViewModel
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
    var showDeleteConfirmationDialog by remember { mutableStateOf<MemberUiModel?>(null) }
    var showAddMemberDialogState by remember { mutableStateOf(false) }
    
    // 🆕 Bottom Sheet 상태 관리
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<MemberUiModel?>(null) }

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
                    DebouncedBackButton(
                        onClick = navigationManger::navigateBack
                    )
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
            onMemberMoreClick = { member ->
                selectedMember = member
                showBottomSheet = true
            }
        )
    }

    // 🆕 멤버 옵션 Bottom Sheet
    if (showBottomSheet) {
        MemberOptionsBottomSheet(
            member = selectedMember!!,
            currentUserId = uiState.currentUserId,
            onDismiss = { 
                showBottomSheet = false
                selectedMember = null
            },
            onEditMember = { member ->
                viewModel.onMemberClick(member)
                showBottomSheet = false
                selectedMember = null
            },
            onDeleteMember = { member ->
                showDeleteConfirmationDialog = member
                showBottomSheet = false
                selectedMember = null
            }
        )
    }

    // 멤버 삭제 확인 다이얼로그
    showDeleteConfirmationDialog?.let { memberUiModel ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = null },
            title = { Text("멤버 내보내기") },
            text = { Text("${memberUiModel.userName.value}님을 프로젝트에서 내보내시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDeleteMember(memberUiModel)
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
 * 🆕 멤버 옵션 Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberOptionsBottomSheet(
    member: MemberUiModel,
    currentUserId: UserId?,
    onDismiss: () -> Unit,
    onEditMember: (MemberUiModel) -> Unit,
    onDeleteMember: (MemberUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    // 🚨 자기 자신인지 확인
    val isSelf = currentUserId?.value == member.userId.value

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 멤버 정보 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserProfileImage(
                    userId = member.userId.value,
                    contentDescription = "${member.userName.value}님의 프로필",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = member.userName.value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (member.roleNames.isNotEmpty()) {
                        Text(
                            text = member.roleNames.joinToString(", ") { it.value },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            Spacer(modifier = Modifier.height(8.dp))

            // 편집 옵션
            MemberOptionItem(
                icon = Icons.Filled.Edit,
                title = "멤버 편집",
                subtitle = "역할 및 권한 수정",
                onClick = { onEditMember(member) }
            )

            // 🚨 자기 자신이 아닌 경우에만 제거 옵션 표시
            if (!isSelf) {
                MemberOptionItem(
                    icon = Icons.Filled.Delete,
                    title = "멤버 내보내기",
                    subtitle = "프로젝트에서 제거",
                    onClick = { onDeleteMember(member) },
                    isDestructive = true
                )
            }

            // 하단 여백
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 🆕 Bottom Sheet 옵션 아이템
 */
@Composable
private fun MemberOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDestructive) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * MemberListContent: 멤버 목록 UI (Stateless)
 */
@Composable
fun MemberListContent(
    paddingValues: PaddingValues,
    uiState: MemberListUiState,
    onSearchQueryChanged: (String) -> Unit,
    onMemberClick: (MemberUiModel) -> Unit,
    onMemberMoreClick: (MemberUiModel) -> Unit,
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
                    member = member,
                    currentUserId = uiState.currentUserId,
                    onClick = { onMemberClick(member) },
                    onMoreClick = { onMemberMoreClick(member) }
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
    member: MemberUiModel,
    currentUserId: UserId?,
    onClick: (MemberUiModel) -> Unit,
    onMoreClick: (MemberUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(member) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserProfileImage(
            userId = member.userId.value,
            contentDescription = "${member.userName.value}님의 프로필 사진",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.userName.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (member.roleNames.isNotEmpty()) {
                Text(
                    text = member.roleNames.joinToString(", ") { it.value },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 🔄 모든 멤버에게 더보기 버튼 표시 (Bottom Sheet에서 제어)
        IconButton(onClick = { onMoreClick(member) }) {
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