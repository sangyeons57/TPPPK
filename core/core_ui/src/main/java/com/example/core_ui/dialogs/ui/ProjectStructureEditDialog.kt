package com.example.core_ui.dialogs.ui

import androidx.lifecycle.viewmodel.compose.viewModel

// Import for DraggableList

/**
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
    projectId: String,
    viewModel: ProjectStructureEditDialogViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    // val listState = rememberLazyListState() // Now managed by DraggableListState
    
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
    
    // ConfirmationDialog 표시 (새로운 방식으로 변경)
    uiState.deleteConfirmationInfo?.let { confirmationInfo ->
        ConfirmationDialog(
            state = ConfirmationDialogState( // 직접 ConfirmationDialogState 구성
                isVisible = true,
                title = confirmationInfo.title,
                message = confirmationInfo.message,
                confirmButtonText = "삭제", // 필요에 따라 변경 가능
                dismissButtonText = "취소" // 필요에 따라 변경 가능
            ),
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() }
        )
    }
    
    ModalBottomSheet(
        onDismissRequest = { viewModel.onCancelChangesClicked() },
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
                DebouncedBackButton(
                    modifier = Modifier.align(Alignment.CenterStart),
                    onClick = { viewModel.onCancelChangesClicked() },
                    contentDescription = "닫기"
                )

                // 우측에 저장 버튼 추가
                IconButton(
                    onClick = { viewModel.onSaveChangesClicked() },
                    enabled = true, // TODO: Replace with viewModel.calculateHasChanges() or similar
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "저장",
                        // tint = if (viewModel.calculateHasChanges()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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
                    text = "아이템 좌측 핸들을 드래그하여 위치를 변경할 수 있습니다. 카테고리 안으로 채널을 넣거나 직속 채널로 뺄 수 있습니다.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // DraggableList 사용
            val draggableListState = rememberDraggableListState(
                initialItems = uiState.draggableItems,
                onItemMove = { id, fromIndex, toIndex, newParentId, newDepth ->
                    viewModel.handleItemMove(id, fromIndex, toIndex, newParentId, newDepth)
                }
            )

            LaunchedEffect(uiState.draggableItems) {
                // Ensure DraggableListState is updated if ViewModel's list changes externally
                // (e.g. after add/delete operations, or if initial load was delayed)
                if (draggableListState.items != uiState.draggableItems) {
                     draggableListState.updateItems(uiState.draggableItems)
                }
            }

            Box(modifier = Modifier.weight(1f)) { // Ensure DraggableList takes available space
                DraggableList(
                    state = draggableListState,
                    modifier = Modifier.fillMaxSize(), // Fill the Box
                    indentationPerDepth = 24.dp,
                    itemContent = { index, itemData, isCurrentlyDragging, listState ->
                        DraggableListItem(
                            itemData = itemData,
                            index = index,
                            isCurrentlyDragging = isCurrentlyDragging,
                            draggableListState = listState,
                            indentationUnit = 24.dp, // Match DraggableList's indentation
                            modifier = Modifier.fillMaxWidth()
                                .padding(vertical = 4.dp) // Padding for each item
                                .shadow(if (isCurrentlyDragging) 8.dp else 1.dp, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    color = if (itemData.originalData is ProjectStructureDraggableItem.CategoryDraggable) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            // dragHandle = { Icon(Icons.Default.DragIndicator, null) } // Default is already this
                            content = { // BoxScope
                                when (val originalItem = itemData.originalData) {
                                    is ProjectStructureDraggableItem.CategoryDraggable -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 12.dp), // Adjusted padding
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = originalItem.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            // 확장/축소 아이콘 (기능은 ViewModel에서 expandedCategories와 draggableItems 필터링으로 처리 필요)
                                            // IconButton(onClick = { viewModel.toggleCategoryExpand(originalItem.id) }) {
                                            //     val isExpanded = uiState.expandedCategories.getOrDefault(originalItem.id, true)
                                            //     Icon(
                                            //         imageVector = Icons.Default.ArrowDropDown,
                                            //         contentDescription = "Expand/Collapse",
                                            //         modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                                            //     )
                                            // }
                                            IconButton(onClick = {
                                                viewModel.openContextMenu(
                                                    ContextMenuState.Category(
                                                        categoryId = originalItem.id,
                                                        position = Offset.Zero
                                                    )
                                                )
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "More options"
                                                )
                                            }
                                        }
                                        // TODO: Display channels under this category if expanded
                                        // This requires filtering draggableItems based on parentId and expanded state
                                    }
                                    is ProjectStructureDraggableItem.ChannelDraggable -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp), // Adjusted padding
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = originalItem.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = {
                                                viewModel.openContextMenu(
                                                    ContextMenuState.Channel(
                                                        categoryId = originalItem.currentParentCategoryId ?: "", // Handle direct channels
                                                        channelId = originalItem.id,
                                                        position = Offset.Zero
                                                    )
                                                )
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "More options"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                )
            } // End of Box with weight for DraggableList
            
            // 카테고리 추가 버튼 (DraggableList 외부)
             Button(
                onClick = { 
                    // TODO: Implement a dialog to get category name, then call:
                    // viewModel.addCategory("새 카테고리 이름 입력받아야 함") 
                    // For now, using a placeholder name. In a real app, you'd open an input dialog first.
                    viewModel.addCategory("새 카테고리") 
                },
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
        } // End of Column for ModalBottomSheet content
    } // End of ModalBottomSheet
    
    // 이름 변경 대화상자
    uiState.renameDialogState?.let { state ->
        val currentItem = uiState.draggableItems.find { 
            when(state) {
                is RenameDialogState.Category -> it.id == state.categoryId && it.originalData is ProjectStructureDraggableItem.CategoryDraggable
                is RenameDialogState.Channel -> it.id == state.channelId && it.originalData is ProjectStructureDraggableItem.ChannelDraggable
            }
        }?.originalData
        
        RenameDialog(
            title = when(state) {
                is RenameDialogState.Category -> "카테고리 이름 변경"
                is RenameDialogState.Channel -> "채널 이름 변경"
            },
            initialValue = currentItem?.name ?: "",
            onDismiss = { viewModel.closeRenameDialog() },
            onConfirm = { newName ->
                viewModel.handleRenameConfirm(newName)
            }
        )
    }
    
    // 채널 추가 대화상자
    uiState.addChannelDialogTargetCategoryId?.let { categoryId ->
        AddChannelDialog(
            onDismiss = { viewModel.closeAddChannelDialog() },
            onConfirm = { channelName, channelMode ->
                viewModel.handleAddChannelConfirm(channelName, channelMode)
            }
        )
    }
    
    // 컨텍스트 메뉴
    uiState.contextMenuState?.let { state ->
        ContextMenu(
            state = state,
            onDismiss = { viewModel.closeContextMenu() },
            onRename = {
                viewModel.handleContextMenuRename()
            },
            onDelete = {
                viewModel.handleContextMenuDelete()
            }
        )
    }

    /**
    // 카테고리 추가 다이얼로그
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { viewModel.closeAddCategoryDialog() },
            onConfirm = { categoryName ->
                viewModel.performAddCategory(categoryName)
            }
        )
    }
    **/
}

// hasChanges 함수: 원본 카테고리와 현재 카테고리를 비교하여 변경사항이 있는지 확인
@Composable
private fun hasChanges(uiState: ProjectStructureEditUiState): Boolean {
    return uiState.categories != uiState.originalCategories
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
    onConfirm: (channelName: String, channelMode: ProjectChannelType) -> Unit
) {
    var channelName by remember { mutableStateOf("") }
    var selectedChannelMode by remember { mutableStateOf(ProjectChannelType.MESSAGES) }
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
                        ProjectChannelType.entries.forEach { mode ->
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

@Preview(showBackground = true)
@Composable
fun ProjectStructureEditDialogPreview() {
    // Mocking DraggableListItemData would be complex here.
    // A simpler preview or a preview-specific ViewModel setup is needed.
    val mockViewModel = viewModel<ProjectStructureEditDialogViewModel>()
    // Set up mockViewModel.uiState with some draggableItems for preview if possible
    // For instance:
    // val sampleCategory = Category(...)
    // val sampleChannel = Channel(...)
    // mockViewModel._uiState.update { it.copy(draggableItems = listOf(
    //     DraggableListItemData(id="cat1", originalData = ProjectStructureDraggableItem.CategoryDraggable(sampleCategory), depth = 0),
    //     DraggableListItemData(id="chan1", originalData = ProjectStructureDraggableItem.ChannelDraggable(sampleChannel, "cat1"), depth = 1)
    // )) }


    TeamnovaPersonalProjectProjectingKotlinTheme {
        ProjectStructureEditDialog(
            onDismiss = {},
            projectId = "preview-project-id",
            viewModel = mockViewModel 
        )
    }
}
        */