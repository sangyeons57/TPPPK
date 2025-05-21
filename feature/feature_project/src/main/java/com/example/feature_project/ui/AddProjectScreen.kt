package com.example.feature_project.ui

import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.AppNavigator
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_project.viewmodel.AddProjectEvent
import com.example.feature_project.viewmodel.AddProjectMode
import com.example.feature_project.viewmodel.AddProjectUiState
import com.example.feature_project.viewmodel.AddProjectViewModel
import com.example.feature_project.viewmodel.CreateProjectMode
import kotlinx.coroutines.flow.collectLatest

/**
 * AddProjectScreen: 상태 관리 및 이벤트 처리 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class) // Scaffold, TopAppBar 등 사용
@Composable
fun AddProjectScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: AddProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리 (스낵바, 네비게이션)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddProjectEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                // 성공 시 뒤로가기 이벤트는 UiState의 플래그로 처리
            }
        }
    }

    // 에러 메시지 스낵바로 표시
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            snackbarHostState.showSnackbar(uiState.errorMessage!!)
            viewModel.errorMessageShown()
        }
    }

    // 프로젝트 추가 성공 시 뒤로 가기
    LaunchedEffect(uiState.projectAddedSuccessfully) {
        if (uiState.projectAddedSuccessfully) {
            appNavigator.navigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("프로젝트 추가") },
                navigationIcon = {
                    DebouncedBackButton(onClick = {
                        appNavigator.navigateBack()
                    })
                }
            )
        }
    ) { paddingValues ->
        AddProjectContent(
            modifier = Modifier.padding(paddingValues), // Scaffold 패딩 적용
            uiState = uiState,
            onModeSelect = viewModel::onModeSelect,
            onCreateModeSelect = viewModel::onCreateModeSelect,
            onJoinCodeChange = viewModel::onJoinCodeChange,
            onProjectNameChange = viewModel::onProjectNameChange,
            onProjectDescriptionChange = viewModel::onProjectDescriptionChange,
            onJoinProjectClick = viewModel::onJoinProjectClick,
            onCreateProjectClick = viewModel::onCreateProjectClick
        )
    }
}

/**
 * AddProjectContent: UI 렌더링 (Stateless)
 */
@OptIn(ExperimentalMaterial3Api::class) // TabRow 등 사용
@Composable
fun AddProjectContent(
    modifier: Modifier = Modifier,
    uiState: AddProjectUiState,
    onModeSelect: (AddProjectMode) -> Unit,
    onCreateModeSelect: (CreateProjectMode) -> Unit,
    onJoinCodeChange: (String) -> Unit,
    onProjectNameChange: (String) -> Unit,
    onProjectDescriptionChange: (String) -> Unit,
    onJoinProjectClick: () -> Unit,
    onCreateProjectClick: () -> Unit
) {
    val modes = AddProjectMode.values() // [JOIN, CREATE]

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // 스크롤 가능하게
            .padding(16.dp)
    ) {
        // 모드 선택 탭 (RadioGroup 대신 TabRow 사용)
        TabRow(selectedTabIndex = uiState.selectedMode.ordinal) {
            modes.forEach { mode ->
                Tab(
                    selected = uiState.selectedMode == mode,
                    onClick = { onModeSelect(mode) },
                    text = { Text(if (mode == AddProjectMode.JOIN) "참여 코드 입력" else "새 프로젝트 생성") }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 참여(Join) 섹션 ---
        // AnimatedVisibility를 사용하면 부드럽게 보이고 사라짐 (선택 사항)
        AnimatedVisibility(visible = uiState.selectedMode == AddProjectMode.JOIN) {
            Column {
                OutlinedTextField(
                    value = uiState.joinCode,
                    onValueChange = onJoinCodeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("참여 코드") },
                    placeholder = { Text("받은 참여 코드를 입력하세요") },
                    singleLine = true,
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onJoinProjectClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.joinCode.isNotBlank() && !uiState.isLoading
                ) {
                    if (uiState.isLoading && uiState.selectedMode == AddProjectMode.JOIN) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    } else {
                        Text("프로젝트 참여")
                    }
                }
            }
        }

        // --- 생성(Create) 섹션 ---
        AnimatedVisibility(visible = uiState.selectedMode == AddProjectMode.CREATE) {
            Column {
                // 모드 선택 탭 (RadioGroup 대신 TabRow 사용)
                TabRow(selectedTabIndex = uiState.createMode.ordinal) {
                    CreateProjectMode.entries.forEach { mode ->
                        Tab(
                            selected = uiState.createMode == mode,
                            onClick = { onCreateModeSelect(mode) },
                            text = { Text(if (mode == CreateProjectMode.OPEN) "공개" else "비공개") }
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.projectName,
                    onValueChange = onProjectNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("프로젝트 이름") },
                    placeholder = { Text("새 프로젝트의 이름을 입력하세요") },
                    singleLine = true,
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.projectDescription,
                    onValueChange = onProjectDescriptionChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), // 여러 줄 입력 가능하도록 높이 설정
                    label = { Text("프로젝트 설명 (선택 사항)") },
                    placeholder = { Text("프로젝트에 대한 간단한 설명을 입력하세요") },
                    maxLines = 3, // 예시: 최대 3줄
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateProjectClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.projectName.isNotBlank() && !uiState.isLoading
                ) {
                    if (uiState.isLoading && uiState.selectedMode == AddProjectMode.CREATE) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    } else {
                        Text("프로젝트 생성")
                    }
                }
            }
        }
    }
}


// --- Preview ---
@Preview(showBackground = true, name = "Add Project - Join Mode")
@Composable
fun AddProjectContentJoinPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AddProjectContent(
            uiState = AddProjectUiState(selectedMode = AddProjectMode.JOIN),
            onModeSelect = {}, onJoinCodeChange = {}, onProjectNameChange = {},
            onProjectDescriptionChange = {}, onJoinProjectClick = {}, onCreateProjectClick = {},
            onCreateModeSelect = {}
        )
    }
}

@Preview(showBackground = true, name = "Add Project - Create Mode")
@Composable
fun AddProjectContentCreatePreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AddProjectContent(
            uiState = AddProjectUiState(selectedMode = AddProjectMode.CREATE),
            onModeSelect = {}, onJoinCodeChange = {}, onProjectNameChange = {},
            onProjectDescriptionChange = {}, onJoinProjectClick = {}, onCreateProjectClick = {},
            onCreateModeSelect = {}
        )
    }
}