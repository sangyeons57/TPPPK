package com.example.feature_task.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.components.draggablelist.DraggableList
import com.example.core_ui.components.draggablelist.DraggableListItem
import com.example.core_ui.components.draggablelist.DraggableListItemData
import com.example.core_ui.components.draggablelist.DraggableListState
import com.example.core_ui.components.draggablelist.rememberDraggableListState
import com.example.core_ui.components.fab.ExtendableFab
import com.example.core_ui.components.fab.FabLabelStyle
import com.example.core_ui.components.fab.FabMenuItem
import com.example.domain.vo.task.TaskType
import com.example.feature_task.model.TaskUiModel
import com.example.feature_task.viewmodel.TaskListViewModel
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
    viewModel: TaskListViewModel = hiltViewModel()
    ) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var fabExpanded by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

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
                            onClick = { isEditMode = !isEditMode }
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
                // Sort all tasks by order field instead of separating by type
                val sortedTasks = uiState.tasks.sortedBy { it.order.value }
                
                // Create DraggableList state - 실시간 순서 기반 처리
                val draggableListState = rememberDraggableListState(
                    initialItems = sortedTasks.map { task ->
                        DraggableListItemData(
                            id = task.id.value,
                            originalData = task
                        )
                    },
                    onItemMove = { _, fromIndex, toIndex ->
                        // 기존 콜백 (호환성을 위해 유지하지만 사용 안함)
                    },
                    onRealtimeReorder = { realtimeOrderedTasks ->
                        // 드래그 완료 시 실시간 순서를 그대로 DB에 저장
                        viewModel.finalizeReorder(realtimeOrderedTasks)
                    }
                )
                
                // Update draggable list state when tasks change (but not during drag)
                LaunchedEffect(sortedTasks) {
                    if (!draggableListState.isDragging) {
                        draggableListState.updateItems(
                            sortedTasks.map { task ->
                                DraggableListItemData(
                                    id = task.id.value,
                                    originalData = task
                                )
                            }
                        )
                    }
                }
                
                DraggableList(
                    state = draggableListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) { index, itemData, isCurrentlyDragging, listState ->
                    Column {
                        TaskItem(
                            task = itemData.originalData,
                            isEditMode = isEditMode,
                            isCurrentlyDragging = isCurrentlyDragging,
                            draggableListState = listState,
                            index = index,
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
                            }
                        )
                        
                        // Add spacing between items
                        if (index < draggableListState.items.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
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
    isEditMode: Boolean,
    isCurrentlyDragging: Boolean = false,
    draggableListState: DraggableListState<TaskUiModel>? = null,
    index: Int = -1,
    currentEditingTaskId: String?,
    editingContentMap: Map<String, String>,
    onEditingStateChange: (String) -> Unit,
    onContentChange: (String, String) -> Unit,
    onStatusChange: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    // Global state-based editing status
    val isEditingThisCard = currentEditingTaskId == task.id.value
    val editingContent = editingContentMap[task.id.value] ?: task.content.value


    // Wrap with DraggableListItem when in edit mode and draggable state is available
    if (isEditMode && draggableListState != null && index >= 0) {
        DraggableListItem(
            itemData = DraggableListItemData(
                id = task.id.value,
                originalData = task
            ),
            index = index,
            isCurrentlyDragging = isCurrentlyDragging,
            draggableListState = draggableListState,
            dragHandle = {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "순서 변경",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        ) {
            TaskCard(
                task = task,
                isEditMode = isEditMode,
                isEditingThisCard = isEditingThisCard,
                editingContent = editingContent,
                onEditingStateChange = onEditingStateChange,
                onContentChange = onContentChange,
                onStatusChange = onStatusChange,
                onDelete = onDelete,
                isCurrentlyDragging = isCurrentlyDragging
            )
        }
    } else {
        TaskCard(
            task = task,
            isEditMode = isEditMode,
            isEditingThisCard = isEditingThisCard,
            editingContent = editingContent,
            onEditingStateChange = onEditingStateChange,
            onContentChange = onContentChange,
            onStatusChange = onStatusChange,
            onDelete = onDelete,
            isCurrentlyDragging = false
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskUiModel,
    isEditMode: Boolean,
    isEditingThisCard: Boolean,
    editingContent: String,
    onEditingStateChange: (String) -> Unit,
    onContentChange: (String, String) -> Unit,
    onStatusChange: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    isCurrentlyDragging: Boolean = false
) {
    val focusRequester = remember(task.id) { FocusRequester() }

    // Auto-focus when editing starts - use try-catch for safety
    LaunchedEffect(isEditingThisCard) {
        if (isEditingThisCard) {
            try {
                kotlinx.coroutines.delay(50) // Small delay to ensure composition is complete
                focusRequester.requestFocus()
            } catch (e: IllegalStateException) {
                // Ignore focus request errors during composition
                android.util.Log.w("TaskCard", "Focus request failed: ${e.message}")
            }
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentlyDragging) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrentlyDragging -> MaterialTheme.colorScheme.primaryContainer
                isEditMode -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
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
                if (task.taskType.isCheckbox()) {
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
                    if (isEditMode && isEditingThisCard) {
                        // 편집 중: OutlinedTextField 사용
                        val focusManager = LocalFocusManager.current
                        
                        OutlinedTextField(
                            value = editingContent,  // Use global editing content
                            onValueChange = { newContent ->
                                onContentChange(task.id.value, newContent)  // Update global state
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    onEditingStateChange(task.id.value)  // End editing
                                    focusManager.clearFocus()
                                }
                            ),
                            minLines = 1,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                    } else if (isEditMode) {
                        // 편집 모드이지만 편집 중이 아님: 클릭 가능한 텍스트
                        Text(
                            text = task.content.value,  // Use Room Flow value
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (task.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp) // Add padding to make click area larger
                                .clickable {
                                    // Start editing when clicked
                                    onEditingStateChange(task.id.value)
                                }
                        )
                    } else {
                        // 보기 모드: 일반 텍스트
                        Text(
                            text = task.content.value,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (task.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        // 체크된 작업인 경우 체크한 사람과 시간 표시
                        if (task.isCompleted && task.checkedBy != null && task.checkedAt != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "체크됨: ${task.checkedByName ?: task.checkedBy!!.internalValue} • ${formatTime(task.checkedAt!!)}",
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
