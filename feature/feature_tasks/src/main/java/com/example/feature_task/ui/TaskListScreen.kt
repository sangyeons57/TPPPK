package com.example.feature_task.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.components.fab.ExtendableFab
import com.example.core_ui.components.fab.FabMenuItem
import com.example.core_ui.components.fab.FabLabelStyle
import com.example.feature_task.viewmodel.TaskListViewModel
import com.example.feature_task.model.TaskUiModel
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 작업 목록 화면
 * Google Keep 스타일의 간단한 메모 리스트
 * Type별로 그룹화되어 표시됨
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    modifier: Modifier = Modifier,
    viewModel: TaskListViewModel = hiltViewModel(),
    ) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var fabExpanded by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text("작업 관리") 
                },
                actions = {
                    IconButton(
                        onClick = { isEditMode = !isEditMode }
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "보기 모드" else "편집 모드",
                            tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            // 편집 모드일 때만 FAB 표시 (보기 모드에서는 숨김)
            if (isEditMode) {
                TaskCreationFab(
                    expanded = fabExpanded,
                    onExpandedChange = { fabExpanded = it },
                    onCreateTask = { taskType ->
                        viewModel.createTask(
                            content = if (taskType == TaskType.CHECKLIST) "새 체크리스트" else "새 메모",
                            taskType = taskType
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "작업이 없습니다",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "+ 버튼을 눌러 첫 작업을 만들어보세요",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sort all tasks by order field instead of separating by type
                    val sortedTasks = uiState.tasks.sortedBy { it.order.value }
                    
                    items(sortedTasks) { task ->
                        TaskItem(
                            task = task,
                            isEditMode = isEditMode,
                            onStatusChange = { taskId, isCompleted ->
                                viewModel.updateTaskStatus(taskId, isCompleted)
                            },
                            onEdit = { taskId, content ->
                                viewModel.editTask(taskId, content)
                            },
                            onDelete = { taskId ->
                                viewModel.deleteTask(taskId)
                            }
                        )
                    }
                }
            }
        }
        
        // Error dialog
        uiState.errorMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("오류") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.clearError() }
                    ) {
                        Text("확인")
                    }
                }
            )
        }
        
    }
}

@Composable
fun TaskItem(
    task: TaskUiModel,
    isEditMode: Boolean,
    onStatusChange: (String, Boolean) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var editingContent by remember { mutableStateOf(task.content.value) }
    var isFocused by remember { mutableStateOf(false) }
    
    // Auto-save when content changes and not focused (debounced)
    LaunchedEffect(editingContent, isFocused) {
        if (!isFocused && editingContent.trim() != task.content.value && editingContent.trim().isNotBlank()) {
            onEdit(task.id.value, editingContent.trim())
        }
    }
    
    // Update local state when task content changes
    LaunchedEffect(task.content.value) {
        editingContent = task.content.value
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEditMode) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (task.taskType == TaskType.CHECKLIST) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = if (isEditMode) null else { isChecked ->
                            onStatusChange(task.id.value, isChecked)
                        },
                        enabled = !isEditMode
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    if (isEditMode) {
                        val focusManager = LocalFocusManager.current
                        
                        OutlinedTextField(
                            value = editingContent,
                            onValueChange = { editingContent = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    isFocused = focusState.isFocused
                                },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                }
                            ),
                            minLines = 1,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                    } else {
                        Text(
                            text = if (task.title.isNotBlank()) task.title else task.content.value,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (task.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        if (task.taskType == TaskType.CHECKLIST && task.description.isNotBlank()) {
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // 체크된 작업인 경우 체크한 사람과 시간 표시
                        if (task.isCompleted && task.checkedBy != null && task.checkedAt != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "체크됨: ${task.checkedBy!!.internalValue} • ${formatTime(task.checkedAt!!)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            
            if (isEditMode) {
                IconButton(
                    onClick = { onDelete(task.id.value) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}


@Composable
fun TaskCreationFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreateTask: (TaskType) -> Unit
) {
    val menuItems = remember {
        listOf(
            FabMenuItem(
                icon = Icons.Default.CheckBox,
                text = "체크리스트",
                contentDescription = "체크리스트 작업 생성",
                onClick = { 
                    onCreateTask(TaskType.CHECKLIST)
                    onExpandedChange(false)
                }
            ),
            FabMenuItem(
                icon = Icons.Default.Comment,
                text = "메모",
                contentDescription = "메모 작업 생성",
                onClick = { 
                    onCreateTask(TaskType.COMMENT)
                    onExpandedChange(false)
                }
            )
        )
    }
    
    ExtendableFab(
        menuItems = menuItems,
        isExpanded = expanded,
        onExpandedChange = onExpandedChange,
        labelStyle = FabLabelStyle.CARD
    )
}

/**
 * Instant를 사용자 친화적인 시간 문자열로 포맷팅
 */
private fun formatTime(instant: Instant): String {
    val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val now = LocalDateTime.now()
    
    return when {
        localDateTime.toLocalDate() == now.toLocalDate() -> {
            // 오늘인 경우 시간만 표시
            localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
        localDateTime.toLocalDate() == now.toLocalDate().minusDays(1) -> {
            // 어제인 경우
            "어제 ${localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        }
        localDateTime.year == now.year -> {
            // 올해인 경우
            localDateTime.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
        }
        else -> {
            // 다른 해인 경우
            localDateTime.format(DateTimeFormatter.ofPattern("yy/MM/dd HH:mm"))
        }
    }
}
