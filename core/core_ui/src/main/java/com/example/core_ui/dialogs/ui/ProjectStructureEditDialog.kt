package com.example.core_ui.dialogs.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.ChannelMode
import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.core_ui.dialogs.viewmodel.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 프로젝트 구조 편집 다이얼로그
 * 카테고리와 채널을 드래그 앤 드롭으로 이동 및 추가/수정/삭제할 수 있습니다.
 *
 * @param onDismiss 다이얼로그가 닫힐 때 호출되는 콜백
 * @param viewModel ViewModel 객체
 * @param projectId 프로젝트 ID
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectStructureEditDialog(
    onDismiss: () -> Unit,
    viewModel: ProjectStructureEditDialogViewModel,
    projectId: String
) {
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    
    // ViewModel의 상태 구독
    val uiState by viewModel.uiState.collectAsState()
    
    // 로드 시 프로젝트 구조 가져오기
    LaunchedEffect(projectId) {
        viewModel.loadProjectStructure(projectId)
    }
    
    // 이벤트 수집 및 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ProjectStructureEditEvent.Dismissed -> onDismiss()
                is ProjectStructureEditEvent.SavedChanges -> onDismiss()
                else -> {} // 다른 이벤트는 컴포저블 내에서 처리
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // 제목 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "프로젝트 구조 편집",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
                
                // 좌측에 닫기 버튼 추가
                IconButton(
                    onClick = { viewModel.cancelChanges() }, // 취소 시 ViewModel 함수 호출
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // 우측에 저장 버튼 추가
                IconButton(
                    onClick = { viewModel.saveChanges() }, // 저장 시 ViewModel 함수 호출
                    enabled = hasChanges(uiState), // 변경사항 여부 확인
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "저장",
                        tint = if (hasChanges(uiState)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
            
            // 안내 텍스트
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = "카테고리나 채널을 길게 누른 후 드래그하여 위치를 변경할 수 있습니다.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // 카테고리 및 채널 목록
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 카테고리 항목
                    itemsIndexed(
                        items = uiState.categories,
                        key = { _, category -> category.id }
                    ) { index, category ->
                        val isExpanded = uiState.expandedCategories.getOrDefault(category.id, true)
                        
                        // 카테고리 항목
                        CategoryListItem(
                            category = category,
                            isExpanded = isExpanded,
                            isDragging = uiState.isDragging && uiState.draggedCategoryIndex == index,
                            onToggleExpand = {
                                viewModel.toggleCategoryExpand(category.id)
                            },
                            onLongPress = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.startDragCategory(index)
                            },
                            onDrop = { targetIndex ->
                                // 드래그 중인 카테고리가 있을 때만 처리
                                uiState.draggedCategoryIndex?.let { fromIndex ->
                                    // 카테고리 이동 처리
                                    if (fromIndex != targetIndex) {
                                        viewModel.moveCategory(fromIndex, targetIndex)
                                    }
                                    
                                    // 드래그 상태 초기화
                                    viewModel.endDrag()
                                }
                            },
                            onContextMenu = { offset ->
                                viewModel.openContextMenu(ContextMenuState.Category(
                                    categoryId = category.id,
                                    position = offset
                                ))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                        
                        // 채널 항목들 (카테고리가 확장된 경우에만 표시)
                        if (isExpanded) {
                            Column {
                                category.channels.forEachIndexed { channelIndex, channel ->
                                    ChannelListItem(
                                        channel = channel,
                                        isDragging = uiState.isDragging && 
                                            uiState.draggedChannelInfo?.categoryId == category.id && 
                                            uiState.draggedChannelInfo?.channelIndex == channelIndex,
                                        onLongPress = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.startDragChannel(category.id, channelIndex)
                                        },
                                        onDrop = { targetCategoryId, targetIndex ->
                                            // 드래그 중인 채널이 있을 때만 처리
                                            uiState.draggedChannelInfo?.let { info ->
                                                // 채널 이동 처리
                                                if (info.categoryId != targetCategoryId || info.channelIndex != targetIndex) {
                                                    viewModel.moveChannel(
                                                        info.categoryId, 
                                                        info.channelIndex, 
                                                        targetCategoryId, 
                                                        targetIndex
                                                    )
                                                }
                                                
                                                // 드래그 상태 초기화
                                                viewModel.endDrag()
                                            }
                                        },
                                        onContextMenu = { offset ->
                                            viewModel.openContextMenu(ContextMenuState.Channel(
                                                categoryId = category.id,
                                                channelId = channel.id,
                                                position = offset
                                            ))
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                                    )
                                }
                                
                                // 채널 추가 버튼
                                TextButton(
                                    onClick = { viewModel.openAddChannelDialog(category.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "채널 추가",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    // 카테고리 추가 버튼
                    item {
                        Button(
                            onClick = { viewModel.addCategory() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("새 카테고리 추가")
                        }
                    }
                }
            }
        }
    }
    
    // 이름 변경 대화상자
    uiState.renameDialogState?.let { state ->
        RenameDialog(
            title = when(state) {
                is RenameDialogState.Category -> "카테고리 이름 변경"
                is RenameDialogState.Channel -> "채널 이름 변경"
            },
            initialValue = when(state) {
                is RenameDialogState.Category -> uiState.categories.find { it.id == state.categoryId }?.name ?: ""
                is RenameDialogState.Channel -> {
                    val categoryId = (state as RenameDialogState.Channel).categoryId
                    val channelId = state.channelId
                    uiState.categories
                        .find { it.id == categoryId }
                        ?.channels
                        ?.find { it.id == channelId }
                        ?.name ?: ""
                }
            },
            onDismiss = { viewModel.closeRenameDialog() },
            onConfirm = { newName ->
                when(state) {
                    is RenameDialogState.Category -> {
                        viewModel.renameCategory(state.categoryId, newName)
                    }
                    is RenameDialogState.Channel -> {
                        viewModel.renameChannel(state.categoryId, state.channelId, newName)
                    }
                }
                viewModel.closeRenameDialog()
            }
        )
    }
    
    // 채널 추가 대화상자
    uiState.addChannelDialogTargetCategoryId?.let { categoryId ->
        AddChannelDialog(
            onDismiss = { viewModel.closeAddChannelDialog() },
            onConfirm = { channelName, channelMode ->
                viewModel.addChannel(categoryId, channelName, channelMode)
                viewModel.closeAddChannelDialog()
            }
        )
    }
    
    // 컨텍스트 메뉴
    uiState.contextMenuState?.let { state ->
        ContextMenu(
            state = state,
            onDismiss = { viewModel.closeContextMenu() },
            onRename = {
                when(state) {
                    is ContextMenuState.Category -> 
                        viewModel.openRenameCategoryDialog(state.categoryId)
                    is ContextMenuState.Channel -> 
                        viewModel.openRenameChannelDialog(state.categoryId, state.channelId)
                }
                viewModel.closeContextMenu()
            },
            onDelete = {
                when(state) {
                    is ContextMenuState.Category -> {
                        viewModel.deleteCategory(state.categoryId)
                    }
                    is ContextMenuState.Channel -> {
                        viewModel.deleteChannel(state.categoryId, state.channelId)
                    }
                }
                viewModel.closeContextMenu()
            }
        )
    }
}

// hasChanges 함수: 원본 카테고리와 현재 카테고리를 비교하여 변경사항이 있는지 확인
@Composable
private fun hasChanges(uiState: ProjectStructureEditUiState): Boolean {
    return uiState.categories != uiState.originalCategories
}

/**
 * 카테고리 리스트 아이템 컴포저블 (기존 CategoryItem 컴포저블 대체)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryListItem(
    category: Category,
    isExpanded: Boolean,
    isDragging: Boolean,
    onToggleExpand: () -> Unit,
    onLongPress: () -> Unit,
    onDrop: (index: Int) -> Unit = { _ -> },
    onContextMenu: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label = "category_elevation"
    )
    
    Surface(
        modifier = modifier
            .shadow(elevation)
            .combinedClickable(
                onClick = onToggleExpand,
                onLongClick = onLongPress,
                onLongClickLabel = "드래그하여 이동"
            )
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onLongPress() },
                    onDragEnd = { /* 드래그 종료 처리 */ },
                    onDragCancel = { /* 드래그 취소 처리 */ },
                    onDrag = { change, _ -> change.consume() }
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onToggleExpand) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) "접기" else "펼치기",
                    modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                )
            }
            
            IconButton(onClick = { onContextMenu(Offset.Zero) }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "더보기"
                )
            }
        }
    }
}

/**
 * 채널 리스트 아이템 컴포저블 (기존 ChannelItem 컴포저블 대체)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelListItem(
    channel: Channel,
    isDragging: Boolean,
    onLongPress: () -> Unit,
    onDrop: (categoryId: String, index: Int) -> Unit = { _, _ -> },
    onContextMenu: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label = "channel_elevation"
    )
    
    Surface(
        modifier = modifier
            .shadow(elevation)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { /* 채널 클릭 시 작업 (예: 채널 진입) */ },
                onLongClick = onLongPress,
                onLongClickLabel = "드래그하여 이동"
            )
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onLongPress() },
                    onDragEnd = { /* 드래그 종료 처리 */ },
                    onDragCancel = { /* 드래그 취소 처리 */ },
                    onDrag = { change, _ -> change.consume() }
                )
            },
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconText = when (channel.channelMode) {
                ChannelMode.TEXT -> "#"
                ChannelMode.VOICE -> "\uD83D\uDD0A" // 예시: 스피커 아이콘
                else -> "?"
            }
            Text(
                text = iconText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp)
            )
            
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(
                onClick = { onContextMenu(Offset.Zero) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "더보기",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * 이름 변경 대화상자
 */
@Composable
private fun RenameDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/**
 * 채널 추가 대화상자
 * ViewModel의 addChannel 함수 시그니처에 맞춰 채널 이름과 모드를 입력받도록 수정합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddChannelDialog(
    onDismiss: () -> Unit,
    onConfirm: (channelName: String, channelMode: ChannelMode) -> Unit
) {
    var channelName by remember { mutableStateOf("") }
    var selectedChannelMode by remember { mutableStateOf(ChannelMode.TEXT) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 채널 추가") },
        text = {
            Column {
                OutlinedTextField(
                    value = channelName,
                    onValueChange = { channelName = it },
                    label = { Text("채널 이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("채널 유형 선택", style = MaterialTheme.typography.labelMedium)
                
                // ExposedDropdownMenuBox는 실험적 API이므로 대신 간단한 드롭다운 사용
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedChannelMode.name,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("유형") },
                        trailingIcon = { 
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "드롭다운 열기"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                    )
                    
                    // 드롭다운 메뉴
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.95f)
                    ) {
                        ChannelMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.name) },
                                onClick = {
                                    selectedChannelMode = mode
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(channelName, selectedChannelMode) },
                enabled = channelName.isNotBlank()
            ) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/**
 * 컨텍스트 메뉴
 */
@Composable
private fun ContextMenu(
    state: ContextMenuState,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val title = when(state) {
        is ContextMenuState.Category -> "카테고리 옵션"
        is ContextMenuState.Channel -> "채널 옵션"
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TextButton(
                    onClick = onRename,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("이름 변경")
                }
                
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("삭제")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
} 