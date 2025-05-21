package com.example.feature_project.structure.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.AppNavigator
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_project.structure.viewmodel.EditCategoryEvent
import com.example.feature_project.structure.viewmodel.EditCategoryUiState
import com.example.feature_project.structure.viewmodel.EditCategoryViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * EditCategoryScreen: 프로젝트 내 카테고리 이름 수정 및 삭제 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EditCategoryScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: EditCategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditCategoryEvent.NavigateBack -> appNavigator.navigateBack()
                is EditCategoryEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is EditCategoryEvent.ClearFocus -> focusManager.clearFocus()
                is EditCategoryEvent.ShowDeleteConfirmation -> showDeleteConfirmationDialog = true
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
                title = { Text("카테고리 편집") },
                navigationIcon = {
                    DebouncedBackButton(onClick = { appNavigator.navigateBack() })
                },
                actions = {
                    // 삭제 버튼 (로딩 중 아닐 때만 활성화)
                    IconButton(
                        onClick = viewModel::onDeleteClick, // 삭제 확인 다이얼로그 표시 요청
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "카테고리 삭제",
                            tint = MaterialTheme.colorScheme.error // 삭제 아이콘 강조
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // 초기 로딩 처리
        if (uiState.isLoading && uiState.currentCategoryName.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            EditCategoryContent(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onCategoryNameChange = viewModel::onCategoryNameChange,
                onUpdateClick = viewModel::updateCategory
            )
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false }, // 외부 클릭 시 닫기
            title = { Text("카테고리 삭제") },
            text = { Text("정말로 이 카테고리를 삭제하시겠습니까? 카테고리 내의 모든 채널도 함께 삭제됩니다.") },
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
 * EditCategoryContent: 카테고리 편집 UI 요소 (Stateless)
 */
@Composable
fun EditCategoryContent(
    modifier: Modifier = Modifier,
    uiState: EditCategoryUiState,
    onCategoryNameChange: (String) -> Unit,
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
        OutlinedTextField(
            value = uiState.currentCategoryName, // 수정할 현재 이름 표시
            onValueChange = onCategoryNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("카테고리 이름") },
            singleLine = true,
            isError = uiState.error != null
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

        Spacer(modifier = Modifier.weight(1f))

        // 수정 완료 버튼
        Button(
            onClick = onUpdateClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.currentCategoryName.isNotBlank() && !uiState.isLoading // 로딩 중 아닐 때 + 이름 있을 때 활성화
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
private fun EditCategoryContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditCategoryContent(
            uiState = EditCategoryUiState(categoryId = "1", currentCategoryName = "기존 카테고리"),
            onCategoryNameChange = {},
            onUpdateClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Edit Category Loading")
@Composable
private fun EditCategoryContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditCategoryContent(
            uiState = EditCategoryUiState(
                categoryId = "1",
                currentCategoryName = "수정 중...",
                isLoading = true
            ),
            onCategoryNameChange = {},
            onUpdateClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Edit Category Error")
@Composable
private fun EditCategoryContentErrorPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        EditCategoryContent(
            uiState = EditCategoryUiState(
                categoryId = "1",
                currentCategoryName = "",
                error = "이름은 비워둘 수 없습니다."
            ),
            onCategoryNameChange = {},
            onUpdateClick = {}
        )
    }
}