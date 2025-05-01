package com.example.teamnovapersonalprojectprojectingkotlin.feature_project_setting.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete // 삭제 아이콘
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project_setting.viewmodel.* // ViewModel과 상태/이벤트 포함
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * ProjectSettingScreen: 프로젝트 설정 화면 (Stateful)
 * 카테고리/채널 관리, 멤버 관리, 역할 관리 등의 메뉴 제공
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSettingScreen(
    modifier: Modifier = Modifier,
    viewModel: ProjectSettingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToEditCategory: (String, String) -> Unit, // projectId, categoryId
    onNavigateToCreateCategory: (String) -> Unit, // projectId
    onNavigateToEditChannel: (String, String, String) -> Unit, // projectId, categoryId, channelId
    onNavigateToCreateChannel: (String, String) -> Unit, // projectId, categoryId
    onNavigateToMemberList: (String) -> Unit, // projectId
    onNavigateToRoleList: (String) -> Unit // projectId
    // TODO: 다른 네비게이션 콜백 추가 (예: 프로젝트 이름 변경, 프로젝트 삭제 등)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 다이얼로그 상태
    var showDeleteCategoryDialog by remember { mutableStateOf<ProjectCategory?>(null) }
    var showDeleteChannelDialog by remember { mutableStateOf<ProjectChannel?>(null) }
    var showRenameProjectDialog by remember { mutableStateOf(false) } // 프로젝트 이름 변경 다이얼로그
    var showDeleteProjectDialog by remember { mutableStateOf(false) } // 프로젝트 삭제 확인 다이얼로그

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ProjectSettingEvent.NavigateBack -> onNavigateBack()
                is ProjectSettingEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is ProjectSettingEvent.NavigateToEditCategory -> onNavigateToEditCategory(event.projectId, event.categoryId)
                is ProjectSettingEvent.NavigateToCreateCategory -> onNavigateToCreateCategory(event.projectId)
                is ProjectSettingEvent.NavigateToEditChannel -> onNavigateToEditChannel(event.projectId, event.categoryId, event.channelId)
                is ProjectSettingEvent.NavigateToCreateChannel -> onNavigateToCreateChannel(event.projectId, event.categoryId)
                is ProjectSettingEvent.NavigateToMemberList -> onNavigateToMemberList(event.projectId)
                is ProjectSettingEvent.NavigateToRoleList -> onNavigateToRoleList(event.projectId)
                is ProjectSettingEvent.ShowDeleteCategoryConfirm -> showDeleteCategoryDialog = event.category
                is ProjectSettingEvent.ShowDeleteChannelConfirm -> showDeleteChannelDialog = event.channel
                is ProjectSettingEvent.ShowRenameProjectDialog -> showRenameProjectDialog = true
                is ProjectSettingEvent.ShowDeleteProjectConfirm -> showDeleteProjectDialog = true
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("프로젝트 설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
                // TODO: 프로젝트 이름 변경, 프로젝트 삭제 등 추가 작업 버튼 (선택적)
            )
        }
    ) { paddingValues ->
        // 로딩 상태 처리
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("오류: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        } else {
            ProjectSettingContent(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onCategoryEditClick = viewModel::requestEditCategory,
                onCategoryDeleteClick = viewModel::requestDeleteCategory,
                onChannelEditClick = viewModel::requestEditChannel,
                onChannelDeleteClick = viewModel::requestDeleteChannel,
                onAddCategoryClick = viewModel::requestCreateCategory,
                onAddChannelClick = viewModel::requestCreateChannel,
                onManageMembersClick = viewModel::requestManageMembers,
                onManageRolesClick = viewModel::requestManageRoles,
                onRenameProjectClick = viewModel::requestRenameProject, // 프로젝트 이름 변경 요청
                onDeleteProjectClick = viewModel::requestDeleteProject // 프로젝트 삭제 요청
            )
        }
    }

    // 카테고리 삭제 확인 다이얼로그
    showDeleteCategoryDialog?.let { category ->
        AlertDialog(
            onDismissRequest = { showDeleteCategoryDialog = null },
            title = { Text("카테고리 삭제") },
            text = { Text("'${category.name}' 카테고리를 삭제하시겠습니까? 카테고리 내의 모든 채널도 함께 삭제됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDeleteCategory(category.id)
                        showDeleteCategoryDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { showDeleteCategoryDialog = null }) { Text("취소") } }
        )
    }

    // 채널 삭제 확인 다이얼로그
    showDeleteChannelDialog?.let { channel ->
        AlertDialog(
            onDismissRequest = { showDeleteChannelDialog = null },
            title = { Text("채널 삭제") },
            text = { Text("'${channel.name}' 채널을 삭제하시겠습니까? 채널 내의 모든 메시지 기록이 삭제됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDeleteChannel(channel.id)
                        showDeleteChannelDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { showDeleteChannelDialog = null }) { Text("취소") } }
        )
    }

    // 프로젝트 이름 변경 다이얼로그
    if (showRenameProjectDialog) {
        RenameProjectDialog(
            currentName = uiState.projectName,
            onDismiss = { showRenameProjectDialog = false },
            onConfirm = { newName ->
                viewModel.confirmRenameProject(newName)
                showRenameProjectDialog = false
            }
        )
    }

    // 프로젝트 삭제 확인 다이얼로그
    if (showDeleteProjectDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteProjectDialog = false },
            title = { Text("프로젝트 삭제") },
            text = { Text("정말로 '${uiState.projectName}' 프로젝트를 삭제하시겠습니까? 모든 데이터가 영구적으로 삭제되며 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDeleteProject()
                        showDeleteProjectDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { showDeleteProjectDialog = false }) { Text("취소") } }
        )
    }
}

/**
 * ProjectSettingContent: 프로젝트 설정 UI 요소 (Stateless)
 */
@Composable
fun ProjectSettingContent(
    modifier: Modifier = Modifier,
    uiState: ProjectSettingUiState,
    onCategoryEditClick: (String) -> Unit, // categoryId
    onCategoryDeleteClick: (ProjectCategory) -> Unit, // category 객체 전달
    onChannelEditClick: (String, String) -> Unit, // categoryId, channelId
    onChannelDeleteClick: (ProjectChannel) -> Unit, // channel 객체 전달
    onAddCategoryClick: () -> Unit,
    onAddChannelClick: (String) -> Unit, // categoryId
    onManageMembersClick: () -> Unit,
    onManageRolesClick: () -> Unit,
    onRenameProjectClick: () -> Unit,
    onDeleteProjectClick: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 16.dp) // 하단 여백 추가
    ) {
        // --- 일반 설정 ---
        item {
            SettingSectionTitle(title = "일반")
            SettingMenuItem(
                text = "프로젝트 이름 변경",
                onClick = onRenameProjectClick
            )
            SettingMenuItem(
                text = "멤버 관리",
                onClick = onManageMembersClick
            )
            SettingMenuItem(
                text = "역할 관리",
                onClick = onManageRolesClick
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // --- 카테고리 및 채널 관리 ---
        item { SettingSectionTitle(title = "카테고리 및 채널") }

        // 카테고리 및 하위 채널 목록
        uiState.categories.forEach { category ->
            item {
                CategoryHeader(
                    category = category,
                    onEditClick = { onCategoryEditClick(category.id) },
                    onDeleteClick = { onCategoryDeleteClick(category) }, // 삭제 확인 위해 객체 전달
                    onAddChannelClick = { onAddChannelClick(category.id) }
                )
            }
            items(
                items = category.channels,
                key = { channel -> channel.id }
            ) { channel ->
                ChannelItem(
                    channel = channel,
                    onEditClick = { onChannelEditClick(category.id, channel.id) },
                    onDeleteClick = { onChannelDeleteClick(channel) } // 삭제 확인 위해 객체 전달
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) } // 카테고리 간 간격
        }

        // 카테고리 추가 버튼
        item {
            TextButton(
                onClick = onAddCategoryClick,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("카테고리 추가")
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // --- 프로젝트 삭제 ---
        item {
            SettingSectionTitle(title = "프로젝트 관리")
            SettingMenuItem(
                text = "프로젝트 삭제",
                onClick = onDeleteProjectClick,
                isDestructive = true // 빨간색 텍스트 등 강조
            )
        }
    }
}

// 설정 섹션 제목
@Composable
fun SettingSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// 일반 설정 메뉴 아이템
@Composable
fun SettingMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false // 삭제 등 위험 작업 여부
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp), // 클릭 영역 확보
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = if (isDestructive) MaterialTheme.colorScheme.error else LocalContentColor.current,
            style = MaterialTheme.typography.bodyLarge
        )
        if (!isDestructive) { // 일반 메뉴에만 화살표 표시
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) // 구분선
}

// 카테고리 헤더
@Composable
fun CategoryHeader(
    category: ProjectCategory,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddChannelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) // 약간의 배경색
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), // 오른쪽 아이콘 패딩 줄임
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) { // 아이콘 버튼 크기 조절
            Icon(Icons.Filled.Edit, contentDescription = "카테고리 편집", modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onAddChannelClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "채널 추가", modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "카테고리 삭제", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
    }
}

// 채널 아이템
@Composable
fun ChannelItem(
    channel: ProjectChannel,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick) // 채널 클릭 시 편집으로 이동
            .padding(start = 32.dp, end = 4.dp, top = 12.dp, bottom = 12.dp), // 카테고리보다 들여쓰기, 오른쪽 아이콘 패딩 줄임
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 채널 아이콘 (텍스트/음성 구분)
        val icon = if (channel.type == ChannelType.TEXT) Icons.Default.ChatBubbleOutline else Icons.AutoMirrored.Filled.VolumeUp // 예시 아이콘
        Icon(
            imageVector = icon,
            contentDescription = "${channel.type} 채널",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        // 편집 버튼은 Row 클릭으로 대체하고 삭제 버튼만 표시
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "채널 삭제", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 32.dp, end = 16.dp)) // 구분선
}

// 프로젝트 이름 변경 다이얼로그
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameProjectDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("프로젝트 이름 변경") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        error = null // 이름 변경 시 에러 초기화
                    },
                    label = { Text("새 프로젝트 이름") },
                    singleLine = true,
                    isError = error != null
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = newName.trim()
                    if (trimmedName.isBlank()) {
                        error = "프로젝트 이름은 비워둘 수 없습니다."
                    } else if (trimmedName == currentName) {
                        onDismiss() // 변경 없으면 그냥 닫기
                    } else {
                        onConfirm(trimmedName)
                    }
                }
            ) { Text("변경") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun ProjectSettingContentPreview() {
    val previewState = ProjectSettingUiState(
        projectId = "p1",
        projectName = "샘플 프로젝트",
        categories = listOf(
            ProjectCategory("c1", "일반", listOf(
                ProjectChannel("ch1", "잡담", ChannelType.TEXT),
                ProjectChannel("ch2", "공지사항", ChannelType.TEXT)
            )),
            ProjectCategory("c2", "개발", listOf(
                ProjectChannel("ch3", "프론트엔드", ChannelType.TEXT),
                ProjectChannel("ch4", "백엔드", ChannelType.TEXT),
                ProjectChannel("ch5", "개발 회의", ChannelType.VOICE)
            ))
        )
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProjectSettingContent(
            uiState = previewState,
            onCategoryEditClick = {},
            onCategoryDeleteClick = {},
            onChannelEditClick = { _, _ -> },
            onChannelDeleteClick = {},
            onAddCategoryClick = {},
            onAddChannelClick = {},
            onManageMembersClick = {},
            onManageRolesClick = {},
            onRenameProjectClick = {},
            onDeleteProjectClick = {}
        )
    }
}

@Preview
@Composable
private fun RenameProjectDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        RenameProjectDialog(currentName = "기존 프로젝트", onDismiss = { }) {}
    }
}