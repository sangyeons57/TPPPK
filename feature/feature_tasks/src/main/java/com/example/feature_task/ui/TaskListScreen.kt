package com.example.feature_task.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.NavigationManger
import com.example.feature_task.viewmodel.TaskListViewModel
import com.example.feature_task.model.TaskUiModel
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType

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
                    // Group tasks by type
                    val checklistTasks = uiState.tasks.filter { it.taskType == TaskType.CHECKLIST }
                    val commentTasks = uiState.tasks.filter { it.taskType == TaskType.COMMENT }
                    
                    // Checklist section
                    if (checklistTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "체크리스트",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(checklistTasks) { task ->
                            TaskItem(
                                task = task,
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
                    
                    // Comments section
                    if (commentTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "메모",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(commentTasks) { task ->
                            TaskItem(
                                task = task,
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
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        onCheckedChange = { isChecked ->
                            onStatusChange(task.id.value, isChecked)
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Column {
                    Text(
                        text = if (task.title.isNotBlank()) task.title else task.content.value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = if (task.taskType == TaskType.COMMENT) 3 else 1
                    )
                    
                    if (task.taskType == TaskType.CHECKLIST && task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
            
            Row {
                IconButton(
                    onClick = { showEditDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "편집",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(
                    onClick = { onDelete(task.id.value) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // Edit dialog
    if (showEditDialog) {
        TaskEditDialog(
            task = task,
            onDismiss = { showEditDialog = false },
            onSave = { content ->
                onEdit(task.id.value, content)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun TaskEditDialog(
    task: TaskUiModel,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var contentText by remember { mutableStateOf(task.content.value) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("작업 편집") },
        text = {
            OutlinedTextField(
                value = contentText,
                onValueChange = { contentText = it },
                label = { Text("내용") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    onSave(contentText.trim())
                }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
fun TaskCreationFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreateTask: (TaskType) -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "rotation"
    )
    
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Task type buttons (show when expanded)
        if (expanded) {
            TaskTypeFab(
                icon = Icons.Default.CheckBox,
                label = "체크리스트",
                onClick = { 
                    onCreateTask(TaskType.CHECKLIST)
                    onExpandedChange(false)
                }
            )
            
            TaskTypeFab(
                icon = Icons.Default.Comment,
                label = "메모",
                onClick = { 
                    onCreateTask(TaskType.COMMENT)
                    onExpandedChange(false)
                }
            )
        }
        
        // Main FAB
        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.rotate(rotation)
        ) {
            Icon(Icons.Default.Add, contentDescription = "작업 추가")
        }
    }
}

@Composable
fun TaskTypeFab(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Small FAB
        SmallFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}