package com.example.feature_edit_channel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.base.Category
import com.example.feature_edit_channel.viewmodel.EditChannelEvent
import com.example.feature_edit_channel.viewmodel.EditChannelUiState
import com.example.feature_edit_channel.viewmodel.EditChannelViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.text.KeyboardOptions

/**
 * EditChannelScreen: 프로젝트 내 채널 이름, 카테고리, 순서 수정/삭제 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EditChannelScreen(
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
                is EditChannelEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is EditChannelEvent.ClearFocus -> focusManager.clearFocus()
                is EditChannelEvent.ShowDeleteConfirmation -> showDeleteConfirmationDialog = true
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
                title = { Text("채널 편집") },
                navigationIcon = {
                    DebouncedBackButton(onClick = viewModel::navigateBack)
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
                onCategorySelected = viewModel::onCategorySelected,
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChannelContent(
    modifier: Modifier = Modifier,
    uiState: EditChannelUiState,
    onChannelNameChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
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
            isError = uiState.error?.contains("이름") == true
        )

        // 카테고리 선택 드롭다운
        CategoryDropdown(
            selectedCategoryId = uiState.currentCategoryId,
            categories = uiState.availableCategories,
            onCategorySelected = onCategorySelected,
            isLoading = uiState.isLoadingCategories,
            isError = uiState.error?.contains("카테고리") == true
        )


        // 채널 타입 표시 (읽기 전용)
        Column {
            Text(
                "채널 유형",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = when(uiState.currentChannelMode) {
                        ProjectChannelType.MESSAGES -> "텍스트 채널"
                        ProjectChannelType.TASKS -> "작업 채널" 
                        else -> "알 수 없는 채널"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
            enabled = uiState.currentChannelName.isNotBlank() && 
                     uiState.currentCategoryId.isNotBlank() && 
                     !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("수정 완료")
            }
        }
    }
}

/**
 * 카테고리 선택 드롭다운 컴포넌트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selectedCategoryId: String,
    categories: List<Category>,
    onCategorySelected: (String) -> Unit,
    isLoading: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id.value == selectedCategoryId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCategory?.name?.value ?: if (isLoading) "로딩 중..." else "카테고리 선택",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = !isLoading && categories.isNotEmpty()),
            label = { Text("카테고리") },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            isError = isError,
            enabled = !isLoading && categories.isNotEmpty()
        )

        if (!isLoading && categories.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name.value) },
                        onClick = {
                            onCategorySelected(category.id.value)
                            expanded = false
                        }
                    )
                }
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
                currentChannelMode = ProjectChannelType.MESSAGES,
                originalChannelMode = ProjectChannelType.MESSAGES,
                currentCategoryId = "category1",
                availableCategories = emptyList()
            ),
            onChannelNameChange = {},
            onCategorySelected = {},
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
                isLoading = true,
            ),
            onChannelNameChange = {},
            onCategorySelected = {},
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
                error = "이름은 필수입니다.",
            ),
            onChannelNameChange = {},
            onCategorySelected = {},
            onUpdateClick = {}
        )
    }
}