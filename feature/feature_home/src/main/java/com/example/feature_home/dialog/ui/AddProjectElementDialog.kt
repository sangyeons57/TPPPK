package com.example.feature_home.dialog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.base.Category
import com.example.domain.model.enum.ProjectChannelType
import com.example.feature_home.dialog.viewmodel.AddProjectElementDialogEvent
import com.example.feature_home.dialog.viewmodel.AddProjectElementDialogViewModel
import com.example.feature_home.dialog.viewmodel.CreateElementType
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * AddProjectElementDialog: 프로젝트 요소(카테고리/채널) 생성 다이얼로그 Composable
 * 탭을 통해 카테고리와 채널을 생성할 수 있는 전체 화면 다이얼로그
 *
 * @param projectId 프로젝트 ID
 * @param onDismissRequest 다이얼로그 닫기 요청 콜백
 * @param onCategoryCreated 카테고리 생성 완료 콜백
 * @param onChannelCreated 채널 생성 완료 콜백
 * @param viewModel AddProjectElementDialogViewModel 인스턴스
 */
@Composable
fun AddProjectElementDialog(
    projectId: String,
    onDismissRequest: () -> Unit,
    onCategoryCreated: (Category) -> Unit = {},
    onChannelCreated: (com.example.domain.model.base.ProjectChannel) -> Unit = {},
    viewModel: AddProjectElementDialogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // 프로젝트 ID 초기화
    LaunchedEffect(projectId) {
        viewModel.initialize(projectId)
    }

    // 이벤트 처리
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddProjectElementDialogEvent.DismissDialog -> {
                    keyboardController?.hide()
                    onDismissRequest()
                }
                is AddProjectElementDialogEvent.CategoryCreated -> {
                    onCategoryCreated(event.category)
                }
                is AddProjectElementDialogEvent.ChannelCreated -> {
                    onChannelCreated(event.channel)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            keyboardController?.hide()
            viewModel.onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "프로젝트 구조 편집",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 탭 선택
                TabRow(
                    selectedTabIndex = if (uiState.selectedTab == CreateElementType.CATEGORY) 0 else 1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = uiState.selectedTab == CreateElementType.CATEGORY,
                        onClick = { viewModel.onTabChanged(0) },
                        text = { Text("카테고리") }
                    )
                    Tab(
                        selected = uiState.selectedTab == CreateElementType.CHANNEL,
                        onClick = { viewModel.onTabChanged(1) },
                        text = { Text("채널") }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 컨텐츠 영역
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (uiState.selectedTab) {
                        CreateElementType.CATEGORY -> {
                            CategoryCreationContent(
                                categoryName = uiState.categoryName,
                                categoryNameError = uiState.categoryNameError,
                                isLoading = uiState.isLoading,
                                onCategoryNameChanged = viewModel::onCategoryNameChanged,
                                onCreateCategory = viewModel::onCreateCategory
                            )
                        }
                        CreateElementType.CHANNEL -> {
                            ChannelCreationContent(
                                channelName = uiState.channelName,
                                channelNameError = uiState.channelNameError,
                                selectedCategoryId = uiState.selectedCategoryId,
                                selectedChannelType = uiState.selectedChannelType,
                                availableCategories = uiState.availableCategories,
                                isLoading = uiState.isLoading,
                                onChannelNameChanged = viewModel::onChannelNameChanged,
                                onChannelCategoryChanged = viewModel::onChannelCategoryChanged,
                                onChannelTypeChanged = viewModel::onChannelTypeChanged,
                                onCreateChannel = viewModel::onCreateChannel
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 카테고리 생성 컨텐츠
 */
@Composable
private fun CategoryCreationContent(
    categoryName: String,
    categoryNameError: String?,
    isLoading: Boolean,
    onCategoryNameChanged: (String) -> Unit,
    onCreateCategory: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = categoryName,
            onValueChange = onCategoryNameChanged,
            label = { Text("카테고리 이름") },
            placeholder = { Text("예: 일반, 공지사항, 개발") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            isError = categoryNameError != null,
            supportingText = categoryNameError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    onCreateCategory()
                }
            ),
            singleLine = true,
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onCreateCategory,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isLoading && categoryName.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("카테고리 생성")
            }
        }
    }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * 채널 생성 컨텐츠
 */
@Composable
private fun ChannelCreationContent(
    channelName: String,
    channelNameError: String?,
    selectedCategoryId: String?,
    selectedChannelType: ProjectChannelType,
    availableCategories: List<Category>,
    isLoading: Boolean,
    onChannelNameChanged: (String) -> Unit,
    onChannelCategoryChanged: (String?) -> Unit,
    onChannelTypeChanged: (ProjectChannelType) -> Unit,
    onCreateChannel: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 채널 이름 입력
        OutlinedTextField(
            value = channelName,
            onValueChange = onChannelNameChanged,
            label = { Text("채널 이름") },
            placeholder = { Text("예: 일반, 알림, 개발-논의") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            isError = channelNameError != null,
            supportingText = channelNameError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 카테고리 선택
        Text(
            text = "카테고리",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // "카테고리 없음" 옵션
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedCategoryId == null,
                        onClick = { onChannelCategoryChanged(null) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedCategoryId == null,
                    onClick = { onChannelCategoryChanged(null) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("카테고리 없음 (프로젝트 직속 채널)")
            }
            
            // 기존 카테고리 옵션들
            availableCategories.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedCategoryId == category.id.value,
                            onClick = { onChannelCategoryChanged(category.id.value) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedCategoryId == category.id.value,
                        onClick = { onChannelCategoryChanged(category.id.value) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(category.name.value)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 채널 종류 선택
        Text(
            text = "채널 종류",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // 메시지 채널
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedChannelType == ProjectChannelType.MESSAGES,
                        onClick = { onChannelTypeChanged(ProjectChannelType.MESSAGES) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedChannelType == ProjectChannelType.MESSAGES,
                    onClick = { onChannelTypeChanged(ProjectChannelType.MESSAGES) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("메시지 (일반 대화)")
            }
            
            // 테스크 채널
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedChannelType == ProjectChannelType.TASKS,
                        onClick = { onChannelTypeChanged(ProjectChannelType.TASKS) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedChannelType == ProjectChannelType.TASKS,
                    onClick = { onChannelTypeChanged(ProjectChannelType.TASKS) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("테스크 (할 일 관리)")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 생성 버튼
        Button(
            onClick = onCreateChannel,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isLoading && channelName.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("채널 생성")
            }
        }
    }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun AddProjectElementDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AddProjectElementDialog(
            projectId = "project123",
            onDismissRequest = {},
            onCategoryCreated = {},
            onChannelCreated = {}
        )
    }
}