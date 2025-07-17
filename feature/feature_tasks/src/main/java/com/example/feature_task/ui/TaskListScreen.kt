package com.example.feature_task.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.NavigationManger
import com.example.feature_task.viewmodel.TaskListViewModel
import com.example.feature_task.model.TaskUiModel
import com.example.domain.model.vo.task.TaskStatus

/**
 * 작업 목록 화면
 * 
 * 동기화 방식:
 * - 각 테스크 필드가 편집이 완료된 경우 동기화됨
 * - 동기화는 항상 마지막 수정이 우선시 됨
 * - 초기 값이 수정되어 있는 경우 사용자에게 다른 사용자가 수정했다고
 *   덮어씨울지 묻는 다이얼로그를 보여줌
 *   - OK -> 동기화
 *   - NO -> 업데이트 취소하고 데이터 읽어오기
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    modifier: Modifier = Modifier,
    viewModel: TaskListViewModel = hiltViewModel(),
    navigationManger: NavigationManger
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text("작업 관리") 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    viewModel.createTask(title = "새 작업", description = "")
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "작업 추가")
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
                    items(uiState.tasks) { task ->
                        TaskItem(
                            task = task,
                            onStatusChange = { taskId, isCompleted ->
                                viewModel.updateTaskStatus(taskId, isCompleted)
                            },
                            onEdit = { taskId ->
                                viewModel.startEditingTask(taskId)
                            },
                            onSave = { taskId ->
                                viewModel.saveTaskChanges(taskId)
                            },
                            onCancel = { taskId ->
                                viewModel.cancelEditingTask(taskId)
                            },
                            onDelete = { taskId ->
                                viewModel.deleteTask(taskId)
                            },
                            onContentChange = { taskId, content ->
                                viewModel.updateTaskContent(taskId, content)
                            },
                            onConflictResolve = { taskId, overwrite ->
                                viewModel.resolveConflict(taskId, overwrite)
                            },
                            onTaskClick = { taskId ->
                                navigationManger.navigateToTaskDetail(
                                    projectId = uiState.projectId,
                                    channelId = uiState.channelId,
                                    taskId = taskId
                                )
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
    onStatusChange: (String, Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onSave: (String) -> Unit,
    onCancel: (String) -> Unit,
    onDelete: (String) -> Unit,
    onContentChange: (String, String) -> Unit,
    onConflictResolve: (String, Boolean) -> Unit,
    onTaskClick: (String) -> Unit
) {
    var showConflictDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(task.hasConflict) {
        if (task.hasConflict) {
            showConflictDialog = true
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !task.isEditing && !task.isUpdating) { 
                onTaskClick(task.id.value) 
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { isChecked ->
                            onStatusChange(task.id.value, isChecked)
                        },
                        enabled = !task.isEditing && !task.isUpdating
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (task.isEditing) {
                        var editContent by remember { mutableStateOf(task.content.value) }
                        
                        OutlinedTextField(
                            value = editContent,
                            onValueChange = { 
                                editContent = it
                                onContentChange(task.id.value, it)
                            },
                            label = { Text("작업 내용") },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = task.title.takeIf { it.isNotBlank() } ?: "제목 없음",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (task.isCompleted) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            
                            if (task.description.isNotBlank()) {
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Row {
                    if (task.isEditing) {
                        IconButton(
                            onClick = { onSave(task.id.value) },
                            enabled = !task.isUpdating
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "저장")
                        }
                        IconButton(
                            onClick = { onCancel(task.id.value) },
                            enabled = !task.isUpdating
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "취소")
                        }
                    } else {
                        IconButton(
                            onClick = { onEdit(task.id.value) },
                            enabled = !task.isUpdating
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "편집")
                        }
                        IconButton(
                            onClick = { onDelete(task.id.value) },
                            enabled = !task.isUpdating
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제")
                        }
                    }
                    
                    if (task.hasConflict) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "충돌",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    if (task.isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            // Status indicator
            val statusColor = when (task.status) {
                TaskStatus.PENDING -> MaterialTheme.colorScheme.outline
                TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                TaskStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
            }
            
            Text(
                text = when (task.status) {
                    TaskStatus.PENDING -> "대기 중"
                    TaskStatus.IN_PROGRESS -> "진행 중"
                    TaskStatus.COMPLETED -> "완료"
                },
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
    
    // Conflict resolution dialog
    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            title = { Text("충돌 감지") },
            text = { 
                Text("다른 사용자가 이 작업을 수정했습니다. 변경사항을 덮어쓰시겠습니까?") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConflictDialog = false
                        onConflictResolve(task.id.value, true)
                    }
                ) {
                    Text("덮어쓰기")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConflictDialog = false
                        onConflictResolve(task.id.value, false)
                    }
                ) {
                    Text("취소")
                }
            }
        )
    }
}