package com.example.feature_project.structure.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_project.structure.viewmodel.ChannelType
import com.example.feature_project.structure.viewmodel.CreateChannelEvent
import com.example.feature_project.structure.viewmodel.CreateChannelUiState
import com.example.feature_project.structure.viewmodel.CreateChannelViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * CreateChannelScreen: 프로젝트 내 카테고리 아래 새 채널 추가 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelScreen(
    modifier: Modifier = Modifier,
    viewModel: CreateChannelViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // 이벤트 처리 (스낵바, 네비게이션)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is CreateChannelEvent.NavigateBack -> onNavigateBack()
                is CreateChannelEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is CreateChannelEvent.ClearFocus -> focusManager.clearFocus()
            }
        }
    }

    // 생성 성공 시 자동으로 뒤로가기
    LaunchedEffect(uiState.createSuccess) {
        if (uiState.createSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("채널 추가") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        CreateChannelContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onChannelNameChange = viewModel::onChannelNameChange,
            onChannelTypeSelected = viewModel::onChannelTypeSelected,
            onCreateClick = viewModel::createChannel
        )
    }
}

/**
 * CreateChannelContent: 채널 추가 UI 요소 (Stateless)
 */
@Composable
fun CreateChannelContent(
    modifier: Modifier = Modifier,
    uiState: CreateChannelUiState,
    onChannelNameChange: (String) -> Unit,
    onChannelTypeSelected: (ChannelType) -> Unit,
    onCreateClick: () -> Unit
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
            value = uiState.channelName,
            onValueChange = onChannelNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("채널 이름") },
            singleLine = true,
            isError = uiState.error != null // 에러 상태 표시 (이름 관련 에러만 표시하도록 개선 가능)
        )

        // 채널 타입 선택 라디오 버튼 그룹
        Column(modifier = Modifier.selectableGroup()) { // 접근성을 위해 그룹화
            Text(
                "채널 유형",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // RadioButton 높이와 비슷하게
                    .selectable(
                        selected = (uiState.selectedChannelType == ChannelType.TEXT),
                        onClick = { onChannelTypeSelected(ChannelType.TEXT) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp)
            ) {
                RadioButton(
                    selected = (uiState.selectedChannelType == ChannelType.TEXT),
                    onClick = null // Row의 selectable에서 처리
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
                        selected = (uiState.selectedChannelType == ChannelType.VOICE),
                        onClick = { onChannelTypeSelected(ChannelType.VOICE) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp)
            ) {
                RadioButton(
                    selected = (uiState.selectedChannelType == ChannelType.VOICE),
                    onClick = null // Row의 selectable에서 처리
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

        Spacer(modifier = Modifier.weight(1f)) // 버튼 하단 배치

        // 완료 버튼
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.channelName.isNotBlank() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("완료")
            }
        }
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun CreateChannelContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CreateChannelContent(
            uiState = CreateChannelUiState(
                channelName = "새로운-채팅방",
                selectedChannelType = ChannelType.TEXT
            ),
            onChannelNameChange = {},
            onChannelTypeSelected = {},
            onCreateClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Create Channel Voice Selected")
@Composable
private fun CreateChannelContentVoicePreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CreateChannelContent(
            uiState = CreateChannelUiState(
                channelName = "음성 회의 채널",
                selectedChannelType = ChannelType.VOICE
            ),
            onChannelNameChange = {},
            onChannelTypeSelected = {},
            onCreateClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Create Channel Loading")
@Composable
private fun CreateChannelContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CreateChannelContent(
            uiState = CreateChannelUiState(channelName = "만드는 중", isLoading = true),
            onChannelNameChange = {},
            onChannelTypeSelected = {},
            onCreateClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Create Channel Error")
@Composable
private fun CreateChannelContentErrorPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CreateChannelContent(
            uiState = CreateChannelUiState(channelName = "", error = "채널 이름은 필수입니다."),
            onChannelNameChange = {},
            onChannelTypeSelected = {},
            onCreateClick = {}
        )
    }
}