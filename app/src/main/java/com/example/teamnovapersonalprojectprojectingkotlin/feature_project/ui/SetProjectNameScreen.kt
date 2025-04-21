package com.example.teamnovapersonalprojectprojectingkotlin.feature_project.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project.viewmodel.SetProjectNameEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project.viewmodel.SetProjectNameNavigationEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project.viewmodel.SetProjectNameUiState
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project.viewmodel.SetProjectNameViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * SetProjectNameScreen: 새 프로젝트의 이름을 설정하는 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetProjectNameScreen(
    modifier: Modifier = Modifier,
    viewModel: SetProjectNameViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateNext: () -> Unit // 다음 단계로 이동하는 콜백
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // 이벤트 처리 (스낵바, 네비게이션)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SetProjectNameEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is SetProjectNameEvent.ClearFocus -> focusManager.clearFocus()
                is SetProjectNameEvent.Navigate -> {
                    when (event.destination) {
                        SetProjectNameNavigationEvent.NavigateBack -> onNavigateBack()
                        SetProjectNameNavigationEvent.NavigateToNextStep -> onNavigateNext()
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("프로젝트 이름 설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        SetProjectNameContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onProjectNameChange = viewModel::onProjectNameChange,
            onNextClick = {
                viewModel.onNextClick()
            }
        )
    }
}

/**
 * SetProjectNameContent: 프로젝트 이름 설정 UI 요소 (Stateless)
 */
@Composable
fun SetProjectNameContent(
    modifier: Modifier = Modifier,
    uiState: SetProjectNameUiState,
    onProjectNameChange: (String) -> Unit,
    onNextClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "프로젝트 이름을 입력해주세요.",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth() // 왼쪽 정렬
        )

        OutlinedTextField(
            value = uiState.projectName,
            onValueChange = onProjectNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("프로젝트 이름") },
            singleLine = true,
            isError = uiState.error != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), // 키보드 액션 버튼 '완료'
            keyboardActions = KeyboardActions(onDone = { // '완료' 버튼 클릭 시
                focusManager.clearFocus() // 포커스 해제
                onNextClick() // 다음 단계 진행
            })
        )

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

        Button(
            onClick = {
                focusManager.clearFocus() // 버튼 클릭 시 포커스 해제
                onNextClick()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.projectName.isNotBlank() && !uiState.isLoading
        ) {
            if (uiState.isLoading) { // 이름 유효성 검사 등 로딩 상태
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("다음") // 또는 "완료" (프로젝트 생성 플로우에 따라 결정)
            }
        }
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun SetProjectNameContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        SetProjectNameContent(
            uiState = SetProjectNameUiState(projectName = "나의 멋진 프로젝트"),
            onProjectNameChange = {},
            onNextClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Set Project Name Error")
@Composable
private fun SetProjectNameContentErrorPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        SetProjectNameContent(
            uiState = SetProjectNameUiState(projectName = "", error = "프로젝트 이름을 입력해야 합니다."),
            onProjectNameChange = {},
            onNextClick = {}
        )
    }
}