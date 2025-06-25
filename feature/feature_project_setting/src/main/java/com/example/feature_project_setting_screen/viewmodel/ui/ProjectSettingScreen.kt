package com.example.feature_project_setting_screen.viewmodel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.CreateCategoryRoute
import com.example.core_navigation.core.CreateChannelRoute
import com.example.core_navigation.core.EditCategoryRoute
import com.example.core_navigation.core.EditChannelRoute
import com.example.core_navigation.core.MemberListRoute
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.core.RoleListRoute
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.project.ProjectName
import com.example.feature_model.CategoryUiModel
import com.example.feature_model.ChannelUiModel
import com.example.feature_project_setting_screen.viewmodel.viewmodel.ProjectSettingEvent
import com.example.feature_project_setting_screen.viewmodel.viewmodel.ProjectSettingUiState
import com.example.feature_project_setting_screen.viewmodel.viewmodel.ProjectSettingViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * ProjectSettingScreen: 프로젝트 설정 화면 (Stateful)
 * 카테고리/채널 관리, 멤버 관리, 역할 관리 등의 메뉴 제공
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSettingScreen(
    navigationManger: NavigationManger,
    modifier: Modifier = Modifier,
    viewModel: ProjectSettingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 다이얼로그 상태
    var showDeleteCategoryDialog by remember { mutableStateOf<CategoryUiModel?>(null) } // Changed type
    var showDeleteChannelDialog by remember { mutableStateOf<ChannelUiModel?>(null) } // Changed type
    var showDeleteProjectDialog by remember { mutableStateOf(false) } // 프로젝트 삭제 확인 다이얼로그

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ProjectSettingEvent.NavigateBack -> navigationManger.navigateBack()
                is ProjectSettingEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is ProjectSettingEvent.NavigateToEditCategory -> navigationManger.navigateTo(
                    EditCategoryRoute(event.projectId.value, event.categoryId)
                )

                is ProjectSettingEvent.NavigateToCreateCategory -> navigationManger.navigateTo(
                    CreateCategoryRoute(event.projectId.value)
                )

                is ProjectSettingEvent.NavigateToEditChannel -> navigationManger.navigateTo(
                    EditChannelRoute(event.projectId.value, event.categoryId, event.channelId)
                )

                is ProjectSettingEvent.NavigateToCreateChannel -> navigationManger.navigateTo(
                    CreateChannelRoute(event.projectId.value, event.categoryId)
                )

                is ProjectSettingEvent.NavigateToMemberList -> navigationManger.navigateTo(
                    MemberListRoute(event.projectId.value)
                )

                is ProjectSettingEvent.NavigateToRoleList -> navigationManger.navigateTo(
                    RoleListRoute(event.projectId.value)
                )
                is ProjectSettingEvent.ShowDeleteCategoryConfirm -> showDeleteCategoryDialog = event.category
                is ProjectSettingEvent.ShowDeleteChannelConfirm -> showDeleteChannelDialog = event.channel
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
                    DebouncedBackButton(onClick = { navigationManger.navigateBack() })
                }
                // TODO: 프로젝트 이름 변경, 프로젝트 삭제 등 추가 작업 버튼 (선택적)
            )
        }
    ) { paddingValues ->
        // 로딩 상태 처리
        if (uiState.isLoading) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), contentAlignment = Alignment.Center) {
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
    showDeleteCategoryDialog?.let { categoryUiModel -> // Changed variable name and type
        AlertDialog(
            onDismissRequest = { showDeleteCategoryDialog = null },
            title = { Text("카테고리 삭제") },
            text = { Text("'${categoryUiModel.name}' 카테고리를 삭제하시겠습니까? 카테고리 내의 모든 채널도 함께 삭제됩니다.") }, // Used categoryUiModel.name
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDeleteCategory(categoryUiModel) // Pass CategoryUiModel
                        showDeleteCategoryDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { showDeleteCategoryDialog = null }) { Text("취소") } }
        )
    }

    // 채널 삭제 확인 다이얼로그
    showDeleteChannelDialog?.let { channelUiModel -> // Changed variable name and type
        AlertDialog(
            onDismissRequest = { showDeleteChannelDialog = null },
            title = { Text("채널 삭제") },
            text = { Text("'${channelUiModel.name}' 채널을 삭제하시겠습니까? 채널 내의 모든 메시지 기록이 삭제됩니다.") }, // Used channelUiModel.name
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDeleteChannel(channelUiModel) // Pass ChannelUiModel
                        showDeleteChannelDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { showDeleteChannelDialog = null }) { Text("취소") } }
        )
    }

    // 프로젝트 이름 변경 다이얼로그
    if (uiState.showRenameProjectDialog) {
        RenameProjectDialog(
            currentName = uiState.projectName,
            onDismiss = viewModel::dismiss,
            onConfirm = { newName ->
                viewModel.confirmRenameProject(newName)
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
    onCategoryDeleteClick: (CategoryUiModel) -> Unit, // Changed to CategoryUiModel
    onChannelEditClick: (String, String) -> Unit, // categoryId, channelId
    onChannelDeleteClick: (ChannelUiModel) -> Unit, // Changed to ChannelUiModel
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
    category: CategoryUiModel, // Changed to CategoryUiModel
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
            text = category.name.value,
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
    channel: ChannelUiModel, // Changed to ChannelUiModel
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
        val icon =
            if (channel.channelType == ProjectChannelType.MESSAGES) Icons.Default.ChatBubbleOutline else Icons.AutoMirrored.Filled.VolumeUp // Compare with Enum.name
        Icon(
            imageVector = icon,
            contentDescription = "${channel.channelType} 채널", // channelType is String
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = channel.name.value, // Use ChannelUiModel.name
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
    currentName: ProjectName,
    onDismiss: () -> Unit,
    onConfirm: (ProjectName) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("프로젝트 이름 변경") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName.value,
                    onValueChange = {
                        newName = ProjectName(it)
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

@Preview
@Composable
private fun RenameProjectDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        RenameProjectDialog(currentName = ProjectName("기존 프로젝트"), onDismiss = { }) {}
    }
}

@Preview(showBackground = true, name = "ProjectSettingScreen - Light")
@Composable
private fun ProjectSettingScreenPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProjectSettingScreen(
            navigationManger = TODO(),
            modifier = TODO(),
            viewModel = TODO()
        )
    }
}

@Preview(showBackground = true, name = "ProjectSettingContent - Loading")
@Composable
private fun ProjectSettingContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProjectSettingContent(
            uiState = ProjectSettingUiState(
                isLoading = true,
                projectId = TODO(),
                projectName = TODO(),
                categories = TODO(),
                error = TODO(),
                showRenameProjectDialog = TODO(),
                showDeleteProjectDialog = TODO()
            ),
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

@Preview(showBackground = true, name = "ProjectSettingContent - Error")
@Composable
private fun ProjectSettingContentErrorPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProjectSettingContent(
            uiState = ProjectSettingUiState(
                error = "미리보기 에러 메시지입니다.",
                projectId = TODO(),
                projectName = TODO(),
                categories = TODO(),
                isLoading = TODO(),
                showRenameProjectDialog = TODO(),
                showDeleteProjectDialog = TODO()
            ),
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