package com.example.feature_member_list_blocked.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.components.user.UserProfileImage
import com.example.domain.model.ui.data.MemberUiModel
import com.example.feature_member_list_blocked.viewmodel.BlockedMemberListEvent
import com.example.feature_member_list_blocked.viewmodel.BlockedMemberListUiState
import com.example.feature_member_list_blocked.viewmodel.BlockedMemberListViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * BlockedMemberListScreen: 차단된 프로젝트 멤버 목록 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedMemberListScreen(
    modifier: Modifier = Modifier,
    viewModel: BlockedMemberListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showUnblockConfirmationDialog by remember { mutableStateOf<MemberUiModel?>(null) }

    // 🆕 Bottom Sheet 상태 관리
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<MemberUiModel?>(null) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is BlockedMemberListEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("차단된 멤버") },
                navigationIcon = {
                    DebouncedBackButton(
                        onClick = viewModel::navigateBack,
                    )
                }
            )
        }
    ) { paddingValues ->
        BlockedMemberListContent(
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

    // 🆕 멤버 옵션 Bottom Sheet (차단 해제만 가능)
    if (showBottomSheet) {
        BlockedMemberOptionsBottomSheet(
            member = selectedMember!!,
            onDismiss = {
                showBottomSheet = false
                selectedMember = null
            },
            onUnblockMember = { member ->
                viewModel.unblockMember(member)
                showBottomSheet = false
                selectedMember = null
            }
        )
    }

    // 차단 해제 확인 다이얼로그
    showUnblockConfirmationDialog?.let { memberUiModel ->
        AlertDialog(
            onDismissRequest = { showUnblockConfirmationDialog = null },
            title = { Text("차단 해제") },
            text = { Text("${memberUiModel.userName.value}님의 차단을 해제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unblockMember(memberUiModel)
                        showUnblockConfirmationDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("해제")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnblockConfirmationDialog = null }
                ) {
                    Text("취소")
                }
            }
        )
    }
}

/**
 * 차단된 멤버 목록 콘텐츠 (Stateless)
 */
@Composable
private fun BlockedMemberListContent(
    paddingValues: PaddingValues,
    uiState: BlockedMemberListUiState,
    onSearchQueryChanged: (String) -> Unit,
    onMemberClick: (MemberUiModel) -> Unit,
    onMemberMoreClick: (MemberUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(paddingValues)
    ) {
        // 검색 바
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            label = { Text("멤버 검색") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

        // 멤버 목록
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("차단된 멤버를 불러오는 중...")
                    }
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "오류가 발생했습니다",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            uiState.filteredMembers.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "검색 결과가 없습니다" else "차단된 멤버가 없습니다",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredMembers) { member ->
                        BlockedMemberItem(
                            member = member,
                            onClick = { onMemberClick(member) },
                            onMoreClick = { onMemberMoreClick(member) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 차단된 멤버 아이템 컴포넌트 (member_list와 동일한 구조)
 */
@Composable
private fun BlockedMemberItem(
    member: MemberUiModel,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 프로필 이미지
        UserProfileImage(
            userId = member.userId.value,
            contentDescription = member.userName.value,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 멤버 정보
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = member.userName.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "차단됨",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // More 버튼
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "옵션",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 🆕 차단된 멤버 옵션 Bottom Sheet (차단 해제만 가능)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedMemberOptionsBottomSheet(
    member: MemberUiModel,
    onDismiss: () -> Unit,
    onUnblockMember: (MemberUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserProfileImage(
                    userId = member.userId.value,
                    contentDescription = member.userName.value,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = member.userName.value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "차단된 멤버",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider()

            Spacer(modifier = Modifier.height(8.dp))

            // 차단 해제 옵션 (권한 있는 사용자만 이 화면에 접근 가능)
            MemberOptionItem(
                icon = Icons.Filled.Block,
                title = "차단 해제",
                subtitle = "멤버 접근 권한 복구",
                onClick = { onUnblockMember(member) },
                isDestructive = false
            )

            // 하단 여백
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Bottom Sheet 옵션 아이템
 */
@Composable
private fun MemberOptionItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
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
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlockedMemberListScreenPreview() {
    MaterialTheme {
        BlockedMemberListContent(
            paddingValues = PaddingValues(0.dp),
            uiState = BlockedMemberListUiState(
                blockedMembers = emptyList(),
                isLoading = false
            ),
            onSearchQueryChanged = { },
            onMemberClick = { },
            onMemberMoreClick = { }
        )
    }
}