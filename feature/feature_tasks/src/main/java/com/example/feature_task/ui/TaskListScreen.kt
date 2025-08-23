package com.example.feature_task.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.domain.vo.task.TaskType
import com.example.feature_task.viewmodel.TaskListViewModel

/**
 * 작업 목록 화면
 * Google Keep 스타일의 간단한 메모 리스트
 * Type별로 그룹화되어 표시됨
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    modifier: Modifier = Modifier,
    viewModel: TaskListViewModel = hiltViewModel()
    ) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var fabExpanded by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // 권한이 없으면 편집 모드 강제 해제
    LaunchedEffect(uiState.canWrite) {
        if (!uiState.canWrite) isEditMode = false
    }

    // Global editing state management
    var currentEditingTaskId by remember { mutableStateOf<String?>(null) }
    var previousEditingTaskId by remember { mutableStateOf<String?>(null) }
    var editingContentMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Automatic save when editing card changes
    LaunchedEffect(currentEditingTaskId) {
        // Save previous card (if not first edit)
        val prevId = previousEditingTaskId
        if (!prevId.isNullOrEmpty()) {
            val editingContent = editingContentMap[prevId]
            val originalTask = uiState.tasks.find { it.id.value == prevId }
            if (editingContent != null && editingContent != originalTask?.content?.value) {
                android.util.Log.d(
                    "TaskListScreen",
                    "autosave prevId=$prevId newContentHash=${editingContent.hashCode()}"
                )
                viewModel.editTask(prevId, editingContent)
            }
        }

        // Start editing new card
        val currId = currentEditingTaskId
        if (!currId.isNullOrEmpty()) {
            val newTask = uiState.tasks.find { it.id.value == currId }
            newTask?.let {
                editingContentMap = editingContentMap + (currId to it.content.value)
            }
        } else {
            // Clear map when editing complete
            editingContentMap = emptyMap()
        }

        // Update previous ID
        previousEditingTaskId = currentEditingTaskId
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text("작업 관리") 
                },
                navigationIcon = {
                    DebouncedBackButton(
                        onClick = { viewModel.navigateBack() }
                    )
                },
                actions = {
                    if (uiState.canWrite) {
                        IconButton(
                            onClick = {
                                // Finish editing and clear focus when toggling modes
                                if (isEditMode && currentEditingTaskId != null) {
                                    currentEditingTaskId = null
                                    focusManager.clearFocus()
                                }
                                isEditMode = !isEditMode
                            }
                        ) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                                contentDescription = if (isEditMode) "보기 모드" else "편집 모드",
                                tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                        // End any ongoing editing and commit before creating a new task
                        if (currentEditingTaskId != null) {
                            currentEditingTaskId = null
                            focusManager.clearFocus()
                        }
                        viewModel.createTask(
                            content = if (taskType == TaskType.CHECKLIST) "새 체크리스트" else "새 메모",
                            taskType = taskType
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                // Background tap ends editing and commits via LaunchedEffect(currentEditingTaskId)
                .pointerInput(currentEditingTaskId) {
                    detectTapGestures(onTap = {
                        if (currentEditingTaskId != null) {
                            android.util.Log.d(
                                "TaskListScreen",
                                "background tap: end editing and save"
                            )
                            currentEditingTaskId = null
                            focusManager.clearFocus()
                        }
                    })
                }
        ) {
            // Compose-side logging to trace UI collection path
            LaunchedEffect(uiState.tasks) {
                val first = uiState.tasks.firstOrNull()
                android.util.Log.d(
                    "TaskListScreen",
                    "compose collected tasks size=${uiState.tasks.size} firstId=${first?.id?.value} updatedAt=${first?.updatedAt}"
                )
            }
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
                val sortedTasks =
                    remember(uiState.tasks) { uiState.tasks.sortedBy { it.order.value } }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(
                        items = sortedTasks,
                        key = { it.id.value }
                    ) { task ->
                        // Per-item log for diagnosing missed updates
                        LaunchedEffect(task.id, task.updatedAt) {
                            android.util.Log.d(
                                "TaskListScreen",
                                "render item id=${task.id.value} updatedAt=${task.updatedAt} contentHash=${task.content.value.hashCode()}"
                            )
                        }
                        TaskItem(
                            task = task,
                            isEditMode = isEditMode,
                            currentEditingTaskId = currentEditingTaskId,
                            editingContentMap = editingContentMap,
                            onEditingStateChange = { taskId ->
                                currentEditingTaskId =
                                    if (currentEditingTaskId == taskId) null else taskId
                            },
                            onContentChange = { taskId, content ->
                                editingContentMap = editingContentMap + (taskId to content)
                            },
                            onStatusChange = { taskId, isCompleted ->
                                viewModel.updateTaskStatus(taskId, isCompleted)
                            },
                            onDelete = { taskId ->
                                viewModel.deleteTask(taskId)
                            },
                            onMoveUp = { taskId ->
                                val idx = sortedTasks.indexOfFirst { it.id.value == taskId }
                                if (idx > 0) {
                                    val newList = sortedTasks.toMutableList().apply {
                                        val tmp = this[idx - 1]
                                        this[idx - 1] = this[idx]
                                        this[idx] = tmp
                                    }
                                    android.util.Log.d(
                                        "TaskListScreen",
                                        "moveUp id=$taskId toIndex=${idx - 1}"
                                    )
                                    viewModel.finalizeReorder(newList)
                                }
                            },
                            onMoveDown = { taskId ->
                                val idx = sortedTasks.indexOfFirst { it.id.value == taskId }
                                if (idx >= 0 && idx < sortedTasks.lastIndex) {
                                    val newList = sortedTasks.toMutableList().apply {
                                        val tmp = this[idx + 1]
                                        this[idx + 1] = this[idx]
                                        this[idx] = tmp
                                    }
                                    android.util.Log.d(
                                        "TaskListScreen",
                                        "moveDown id=$taskId toIndex=${idx + 1}"
                                    )
                                    viewModel.finalizeReorder(newList)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
 
