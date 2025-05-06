package com.example.feature_project.structure.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_project.structure.viewmodel.CreateCategoryEvent
import com.example.feature_project.structure.viewmodel.CreateCategoryUiState
import com.example.feature_project.structure.viewmodel.CreateCategoryViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * CreateCategoryScreen: 프로젝트 내 새 카테고리 추가 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun CreateCategoryScreen(
    navigationManager: ComposeNavigationHandler,
    modifier: Modifier = Modifier,
    viewModel: CreateCategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // 이벤트 처리 (스낵바, 네비게이션)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is CreateCategoryEvent.NavigateBack -> navigationManager.navigateBack()
                is CreateCategoryEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is CreateCategoryEvent.ClearFocus -> focusManager.clearFocus()
            }
        }
    }

    // 생성 성공 시 자동으로 뒤로가기
    LaunchedEffect(uiState.createSuccess) {
        if (uiState.createSuccess) {
            navigationManager.navigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("카테고리 추가") },
                navigationIcon = {
                    IconButton(onClick = { navigationManager.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        CreateCategoryContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onCategoryNameChange = viewModel::onCategoryNameChange,
            onCreateClick = viewModel::createCategory
        )
    }
}

/**
 * CreateCategoryContent: 카테고리 추가 UI 요소 (Stateless)
 */
@Composable
fun CreateCategoryContent(
    modifier: Modifier = Modifier,
    uiState: CreateCategoryUiState,
    onCategoryNameChange: (String) -> Unit,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // 내용 적어도 스크롤 가능하게
            .padding(horizontal = 16.dp, vertical = 24.dp), // 상하 패딩 추가
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // 요소 간 간격
    ) {
        OutlinedTextField(
            value = uiState.categoryName,
            onValueChange = onCategoryNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("카테고리 이름") },
            singleLine = true,
            isError = uiState.error != null // 에러 상태 표시
        )

        // 에러 메시지 표시
        if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth() // 왼쪽 정렬되도록
            )
        } else {
            // 에러 없을 때 공간 차지 않도록 빈 공간 추가 (선택적)
            Spacer(modifier = Modifier.height(MaterialTheme.typography.bodySmall.lineHeight.value.dp))
        }


        Spacer(modifier = Modifier.weight(1f)) // 버튼을 하단으로 밀기 위한 Spacer

        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.categoryName.isNotBlank() && !uiState.isLoading // 이름이 비어있지 않고 로딩 중이 아닐 때 활성화
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
private fun CreateCategoryContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CreateCategoryContent(
            uiState = CreateCategoryUiState(categoryName = "새 카테고리"),
            onCategoryNameChange = {},
            onCreateClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Create Category Loading")
@Composable
private fun CreateCategoryContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CreateCategoryContent(
            uiState = CreateCategoryUiState(categoryName = "로딩중 카테고리", isLoading = true),
            onCategoryNameChange = {},
            onCreateClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Create Category Error")
@Composable
private fun CreateCategoryContentErrorPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CreateCategoryContent(
            uiState = CreateCategoryUiState(categoryName = "", error = "카테고리 이름은 비워둘 수 없습니다."),
            onCategoryNameChange = {},
            onCreateClick = {}
        )
    }
}