package com.example.feature_project.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.destination.AppRoutes
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_project.viewmodel.JoinProjectEvent
import com.example.feature_project.viewmodel.JoinProjectUiState
import com.example.feature_project.viewmodel.JoinProjectViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * JoinProjectScreen: 초대 코드/링크로 프로젝트에 참여하는 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinProjectScreen(
    navigationManager: ComposeNavigationHandler,
    modifier: Modifier = Modifier,
    viewModel: JoinProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // 이벤트 처리 (스낵바, 네비게이션)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is JoinProjectEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is JoinProjectEvent.JoinSuccess -> {
                    navigationManager.navigate(NavigationCommand.NavigateClearingBackStack(AppRoutes.Main.ROOT))
                }
                is JoinProjectEvent.ClearFocus -> focusManager.clearFocus()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("프로젝트 참여하기") },
                navigationIcon = {
                    IconButton(onClick = { navigationManager.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        JoinProjectContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onCodeOrLinkChange = viewModel::onCodeOrLinkChange,
            onJoinClick = {
                focusManager.clearFocus() // 버튼 클릭 시 키보드 숨김
                viewModel.joinProject()
            }
        )
    }
}

/**
 * JoinProjectContent: 프로젝트 참여 UI 요소 (Stateless)
 */
@Composable
fun JoinProjectContent(
    modifier: Modifier = Modifier,
    uiState: JoinProjectUiState,
    onCodeOrLinkChange: (String) -> Unit,
    onJoinClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween // 요소들을 위아래로 분산
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp) // 내부 요소 간 간격
        ) {
            Text(
                "프로젝트 초대 링크 혹은 코드를 입력해주세요.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start, // 왼쪽 정렬
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp) // 약간의 들여쓰기
            )

            OutlinedTextField(
                value = uiState.inviteCodeOrLink,
                onValueChange = onCodeOrLinkChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("초대 링크 또는 코드") },
                placeholder = { Text("https://... 또는 코드 입력") },
                singleLine = true, // 여러 줄 입력 방지
                isError = uiState.error != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onJoinClick() }) // 완료 시 참여 시도
            )

            if (uiState.error != null) {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 버튼을 하단에 배치
        Button(
            onClick = onJoinClick,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp), // 상단 여백 추가
            enabled = uiState.inviteCodeOrLink.isNotBlank() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("프로젝트 참여하기")
            }
        }
    }
}


// --- Preview ---
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun JoinProjectContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("프로젝트 참여하기") }) }) { padding ->
            JoinProjectContent(
                modifier = Modifier.padding(padding),
                uiState = JoinProjectUiState(inviteCodeOrLink = "https://example.com/invite/abcde"),
                onCodeOrLinkChange = {},
                onJoinClick = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Join Project Initial")
@Composable
private fun JoinProjectContentInitialPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("프로젝트 참여하기") }) }) { padding ->
            JoinProjectContent(
                modifier = Modifier.padding(padding),
                uiState = JoinProjectUiState(),
                onCodeOrLinkChange = {},
                onJoinClick = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Join Project Loading")
@Composable
private fun JoinProjectContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("프로젝트 참여하기") }) }) { padding ->
            JoinProjectContent(
                modifier = Modifier.padding(padding),
                uiState = JoinProjectUiState(inviteCodeOrLink = "valid-code", isLoading = true),
                onCodeOrLinkChange = {},
                onJoinClick = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Join Project Error")
@Composable
private fun JoinProjectContentErrorPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("프로젝트 참여하기") }) }) { padding ->
            JoinProjectContent(
                modifier = Modifier.padding(padding),
                uiState = JoinProjectUiState(
                    inviteCodeOrLink = "invalid-code",
                    error = "유효하지 않은 코드입니다."
                ),
                onCodeOrLinkChange = {},
                onJoinClick = {}
            )
        }
    }
}