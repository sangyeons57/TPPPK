package com.example.feature_project.structure.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.AppNavigator
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_project.structure.viewmodel.EditChannelEvent
import com.example.feature_project.structure.viewmodel.EditChannelUiState
import com.example.feature_project.structure.viewmodel.EditChannelViewModel
import kotlinx.coroutines.flow.collectLatest
import com.example.domain.model.ChannelMode

/**
 * EditChannelScreen: 프로젝트 내 채널 이름 및 유형 수정/삭제 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EditChannelScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: EditChannelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditChannelEvent.NavigateBack -> appNavigator.navigateBack()
                is EditChannelEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is EditChannelEvent.ClearFocus -> focusManager.clearFocus()
                is EditChannelEvent.ShowDeleteConfirmation -> showDeleteConfirmationDialog = true
            }
        }
    }

    // 수정 또는 삭제 성공 시 자동으로 뒤로가기
    LaunchedEffect(uiState.updateSuccess, uiState.deleteSuccess) {
        if (uiState.updateSuccess || uiState.deleteSuccess) {
            appNavigator.navigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("채널 편집") },
                navigationIcon = {
                    DebouncedBackButton(onClick = { appNavigator.navigateBack() })
                },
                actions = {
                    IconButton(
                        onClick = viewModel::onDeleteClick,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "채널 삭제",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // 초기 로딩 처리
        if (uiState.isLoading && uiState.currentChannelName.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            EditChannelContent(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onChannelNameChange = viewModel::onChannelNameChange,
                onChannelTypeSelected = viewModel::onChannelTypeSelected,
                onUpdateClick = viewModel::updateChannel
            )
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("채널 삭제") },
            text = { Text("정말로 이 채널을 삭제하시겠습니까? 채널 내의 모든 메시지 기록이 삭제됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDelete()
                        showDeleteConfirmationDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

/**
 * EditChannelContent: 채널 편집 UI 요소 (Stateless)
 */
@Composable
fun EditChannelContent(
    modifier: Modifier = Modifier,
    uiState: EditChannelUiState,
    onChannelNameChange: (String) -> Unit,
    onChannelTypeSelected: (ChannelMode) -> Unit,
    onUpdateClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 채널 이름 입력 필드
        OutlinedTextField(
            value = uiState.currentChannelName,
            onValueChange = onChannelNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("채널 이름") },
            singleLine = true,
            isError = uiState.error != null // 이름 관련 에러만 표시하도록 개선 가능
        )

        // 채널 타입 선택 라디오 버튼 그룹
        Column(modifier = Modifier.selectableGroup()) {
            Text(
                "채널 유형",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .selectable(
                        selected = (uiState.currentChannelMode == ChannelMode.TEXT),
                        onClick = { onChannelTypeSelected(ChannelMode.TEXT) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp)
            ) {
                RadioButton(
                    selected = (uiState.currentChannelMode == ChannelMode.TEXT),
                    onClick = null
                )
                Text(
                    text = "텍스트 채널",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .selectable(
                        selected = (uiState.currentChannelMode == ChannelMode.VOICE),
                        onClick = { onChannelTypeSelected(ChannelMode.VOICE) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp)
            ) {
                RadioButton(
                    selected = (uiState.currentChannelMode == ChannelMode.VOICE),
                    onClick = null
                )
                Text(
                    text = "음성 채널",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        // 에러 메시지 표시
        if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Spacer(modifier = Modifier.height(MaterialTheme.typography.bodySmall.lineHeight.value.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // 수정 완료 버튼
        Button(
            onClick = onUpdateClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.currentChannelName.isNotBlank() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("수정 완료")
            }
        }
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun EditChannelContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditChannelContent(
            uiState = EditChannelUiState(
                channelId = "1",
                currentChannelName = "기존-채팅방",
                originalChannelName = "기존-채팅방",
                currentChannelMode = ChannelMode.TEXT,
                originalChannelMode = ChannelMode.TEXT
            ),
            onChannelNameChange = {},
            onChannelTypeSelected = {},
            onUpdateClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Edit Channel Loading")
@Composable
private fun EditChannelContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditChannelContent(
            uiState = EditChannelUiState(
                channelId = "1",
                currentChannelName = "수정 중...",
                isLoading = true
            ),
            onChannelNameChange = {},
            onChannelTypeSelected = {},
            onUpdateClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Edit Channel Error")
@Composable
private fun EditChannelContentErrorPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditChannelContent(
            uiState = EditChannelUiState(
                channelId = "1",
                currentChannelName = "",
                error = "이름은 필수입니다."
            ),
            onChannelNameChange = {},
            onChannelTypeSelected = {},
            onUpdateClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Edit Channel Voice Selected")
@Composable
private fun EditChannelContentVoicePreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditChannelContent(
            uiState = EditChannelUiState(
                currentChannelName = "음성 채널 편집",
                currentChannelMode = ChannelMode.VOICE
            ),
            onChannelNameChange = {},
            onChannelTypeSelected = {},
            onUpdateClick = {}
        )
    }
}