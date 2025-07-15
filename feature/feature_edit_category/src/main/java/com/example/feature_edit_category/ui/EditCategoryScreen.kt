package com.example.feature_edit_category.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_edit_category.viewmodel.EditCategoryEvent
import com.example.feature_edit_category.viewmodel.EditCategoryUiState
import com.example.feature_edit_category.viewmodel.EditCategoryViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * EditCategoryScreen: 프로젝트 내 카테고리 이름 수정 및 삭제 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EditCategoryScreen(
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
                is EditCategoryEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is EditCategoryEvent.ClearFocus -> focusManager.clearFocus()
                is EditCategoryEvent.ShowDeleteConfirmation -> showDeleteConfirmationDialog = true
            }
        }
    }

    // 수정 또는 삭제 성공 시 자동으로 뒤로가기
    LaunchedEffect(uiState.updateSuccess, uiState.deleteSuccess) {
        if (uiState.updateSuccess || uiState.deleteSuccess) {
            viewModel.navigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("카테고리 편집") },
                navigationIcon = {
                    DebouncedBackButton(onClick = viewModel::navigateBack)
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
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            EditCategoryContent(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onCategoryNameChange = viewModel::onCategoryNameChange,
                onMoveUp = viewModel::moveCategoryUp,
                onMoveDown = viewModel::moveCategoryDown,
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
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
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
            isError = uiState.error?.contains("이름") == true
        )

        // 카테고리 순서 조절 버튼
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "순서",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "현재 순서: ${uiState.currentCategoryOrder.toInt()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 위로 이동 버튼
                        IconButton(
                            onClick = onMoveUp,
                            enabled = !uiState.isLoading && uiState.canMoveUp
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowUp,
                                contentDescription = "위로 이동",
                                tint = if (uiState.canMoveUp) MaterialTheme.colorScheme.primary 
                                      else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                        
                        // 아래로 이동 버튼
                        IconButton(
                            onClick = onMoveDown,
                            enabled = !uiState.isLoading && uiState.canMoveDown
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = "아래로 이동",
                                tint = if (uiState.canMoveDown) MaterialTheme.colorScheme.primary 
                                      else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
                
                Text(
                    text = "버튼을 사용하여 카테고리 순서를 조정하세요. 낮은 순서일수록 위에 표시됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
            uiState = EditCategoryUiState(categoryId = "1", currentCategoryName = "기존 카테고리", currentCategoryOrder = 1.0, canMoveUp = true, canMoveDown = true),
            onCategoryNameChange = {},
            onMoveUp = {},
            onMoveDown = {},
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
                currentCategoryOrder = 2.0,
                isLoading = true,
                canMoveUp = false,
                canMoveDown = false
            ),
            onCategoryNameChange = {},
            onMoveUp = {},
            onMoveDown = {},
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
                currentCategoryOrder = 0.0,
                error = "이름은 비워둘 수 없습니다.",
                canMoveUp = false,
                canMoveDown = true
            ),
            onCategoryNameChange = {},
            onMoveUp = {},
            onMoveDown = {},
            onUpdateClick = {}
        )
    }
}