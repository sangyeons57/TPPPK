package com.example.core_ui.dialogs.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.util.DateTimeUtil
import com.example.core_ui.components.draggablelist.DraggableListItemData
import com.example.core_ui.dialogs.service.AddChannelService
import com.example.core_ui.dialogs.service.ConvertProjectStructureToDraggableItems
import com.example.core_ui.dialogs.service.DeleteCategoryService
import com.example.core_ui.dialogs.service.DeleteChannelService
import com.example.core_ui.dialogs.service.MoveCategoryService
import com.example.core_ui.dialogs.service.MoveChannelService
import com.example.core_ui.dialogs.service.RenameCategoryService
import com.example.core_ui.dialogs.service.RenameChannelService
import com.example.domain.model.Category
import com.example.domain.model.Channel
import com.example.domain.model.ChannelMode
import com.example.domain.model.ChannelType
import com.example.domain.model.ProjectStructure
import com.example.domain.model.channel.ProjectSpecificData
import com.example.domain.usecase.project.GetProjectStructureUseCase
import com.example.domain.usecase.project.UpdateProjectStructureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 삭제할 아이템의 유형 (카테고리 또는 채널) -> ChannelType으로 변경
 */
enum class ChannelTypeForDeletion { // 이름 충돌 방지를 위해 임시로 ChannelTypeForDeletion으로 변경
    CATEGORY,
    CHANNEL
}

/**
 * 삭제 확인 다이얼로그에 필요한 정보를 담는 데이터 클래스
 */
data class DeleteConfirmationInfo(
    val title: String,
    val message: String,
    val itemType: ChannelTypeForDeletion, // DeleteItemType -> ChannelTypeForDeletion
    val categoryId: String,
    val channelId: String? = null // 채널 삭제 시에만 사용
)

// Sealed interface for draggable items for type safety
sealed interface ProjectStructureDraggableItem {
    val id: String
    val name: String
    // Add other common properties if needed for display or logic not covered by DraggableListItemData

    data class CategoryDraggable(val category: Category) : ProjectStructureDraggableItem {
        override val id: String get() = category.id
        override val name: String get() = category.name
    }

    data class ChannelDraggable(
        val channel: Channel,
        val currentParentCategoryId: String? // Null if it's a direct channel initially or becomes one
    ) : ProjectStructureDraggableItem {
        override val id: String get() = channel.id
        override val name: String get() = channel.name
    }
}

/**
 * 프로젝트 구조 편집 화면의 상태를 관리하는 ViewModel
 */
@HiltViewModel
class ProjectStructureEditDialogViewModel @Inject constructor(
    private val getProjectStructureUseCase: GetProjectStructureUseCase,
    private val updateProjectStructureUseCase: UpdateProjectStructureUseCase,
    private val convertProjectStructureToDraggableItemsService: ConvertProjectStructureToDraggableItems,
    private val moveChannelService: MoveChannelService,
    private val moveCategoryService: MoveCategoryService,
    private val deleteChannelService: DeleteChannelService,
    private val deleteCategoryService: DeleteCategoryService,
    private val addChannelService: AddChannelService,
    private val renameCategoryService: RenameCategoryService,
    private val renameChannelService: RenameChannelService
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow(ProjectStructureEditUiState())
    val uiState: StateFlow<ProjectStructureEditUiState> = _uiState.asStateFlow()

    // 이벤트
    private val _eventFlow = MutableSharedFlow<ProjectStructureEditEvent>()
    val eventFlow: SharedFlow<ProjectStructureEditEvent> = _eventFlow.asSharedFlow()

    // 변경 사항이 있는지 추적
    private var hasChanges = false

    /**
     * 특정 프로젝트의 구조 데이터를 로드합니다.
     * @param projectId 로드할 프로젝트 ID
     */
    fun loadProjectStructure(projectId: String) {
        _uiState.update { it.copy(isLoading = true, error = null, projectId = projectId) }
        viewModelScope.launch {
            try {
                getProjectStructureUseCase(projectId).fold(
                    onSuccess = { structure ->
                        convertProjectStructureToDraggableItemsService(structure).fold(
                            onSuccess = { newDraggableItems ->
                                _uiState.update {
                                    it.copy(
                                        draggableItems = newDraggableItems,
                                        originalDraggableItems = newDraggableItems, // Store original state
                                        isLoading = false,
                                        // Clear categories and directChannels as they are now part of draggableItems
                                        categories = emptyList(),
                                        directChannels = emptyList(),
                                        originalCategories = emptyList(),
                                        originalDirectChannels = emptyList()
                                    )
                                }
                                hasChanges = newDraggableItems.isNotEmpty() // Or a more sophisticated check
                            },
                            onFailure = { e ->
                                _uiState.update {
                                    it.copy(isLoading = false, error = "프로젝트 구조를 불러오지 못했습니다: ${e.message}")
                                }
                            }
                        )
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isLoading = false, error = "프로젝트 구조를 불러오지 못했습니다: ${e.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "프로젝트 구조를 불러오지 못했습니다: ${e.message}")
                }
            }
        }
    }

    // Placeholder for the complex move logic
    fun handleItemMove(id: String, fromIndex: Int, toIndex: Int, newParentId: String?, newDepth: Int) {
        viewModelScope.launch {
            val currentItems = _uiState.value.draggableItems.toMutableList()
            val movedItemData = currentItems.find { it.id == id } ?: return@launch
            val originalItemIndex = currentItems.indexOf(movedItemData)

            if (originalItemIndex == -1) return@launch // Should not happen

            // 1. Create the updated version of the moved item
            val updatedMovedItem = movedItemData.copy(
                parentId = newParentId,
                depth = newDepth,
                originalData = when (val data = movedItemData.originalData) { // Update internal data if needed
                    is ProjectStructureDraggableItem.ChannelDraggable -> data.copy(currentParentCategoryId = newParentId)
                    else -> data
                }
            )

            // 2. Perform the move in the list
            currentItems.removeAt(originalItemIndex)
            currentItems.add(toIndex.coerceIn(0, currentItems.size), updatedMovedItem)

            // 3. **CRITICAL STEP**: Re-evaluate orders, parent-child relationships, and potentially 
            //    the underlying Category/Channel objects for all affected items.
            //    This is highly complex and needs careful implementation.
            //    For now, just update the list and the moved item's basic properties.
            //    A full re-ordering and re-parenting logic is needed here.

            // Example: If a channel moved to a new category, its channel.projectSpecificData.categoryId needs update.
            // If a channel moved to direct, its type and projectSpecificData might need update.
            // All items' 'order' within their respective parent/depth might need recalculation.
            
            // For a robust solution, it might be better to:
            // a. Convert draggableItems back to ProjectStructure (or an intermediate mutable structure).
            // b. Apply the move operation on this structure (which handles nested list updates).
            // c. Convert the modified structure back to draggableItems.
            // This is safer than manipulating the flattened list directly for complex tree operations.

            Log.d("ViewModel", "Item $id moved from $fromIndex to $toIndex. New Parent: $newParentId, New Depth: $newDepth")

            _uiState.update { it.copy(draggableItems = currentItems) }
            // TODO: Set hasChanges based on comparison with originalDraggableItems
            // setHasChanges()
        }
    }

    // TODO: Implement saveChanges to convert draggableItems back to ProjectStructure
    // TODO: Implement addCategory, addChannel, delete operations for draggableItems
    // TODO: Implement setHasChanges based on current and original draggableItems

    // ... (other functions like startDrag, endDrag, rename, delete confirmation will need to be adapted or removed if handled by DraggableListState)
    // The ViewModel's role shifts from managing drag state to managing the list data and responding to move callbacks.

    // Example of how hasChanges might be checked (simplified)
    private fun calculateHasChanges(): Boolean {
        return _uiState.value.draggableItems != _uiState.value.originalDraggableItems
    }

    /**
     * 변경 사항을 저장하고 다이얼로그를 닫습니다.
     * 변경 사항이 없으면 즉시 다이얼로그를 닫습니다.
     */
    fun onSaveChangesClicked() { // Renamed from saveChanges to avoid conflict if we keep old one for a bit
        if (!calculateHasChanges()) {
            viewModelScope.launch { _eventFlow.emit(ProjectStructureEditEvent.Dismissed) }
            return
        }
        // TODO: Convert draggableItems to ProjectStructure and call updateProjectStructureUseCase
        // For now, assume success and emit SavedChanges then Dismissed
        viewModelScope.launch {
            // Simulate saving
            // val newProjectStructure = convertDraggableItemsToProjectStructure(_uiState.value.draggableItems)
            // updateProjectStructureUseCase(_uiState.value.projectId, newProjectStructure).fold(
            // onSuccess = {
            // _eventFlow.emit(ProjectStructureEditEvent.SavedChanges)
            // _eventFlow.emit(ProjectStructureEditEvent.Dismissed)
            // hasChanges = false
            // },
            // onFailure = { e ->
            // _eventFlow.emit(ProjectStructureEditEvent.Error("저장 실패: ${e.message}"))
            // }
            // )
            _eventFlow.emit(ProjectStructureEditEvent.SavedChanges) // Placeholder
            _eventFlow.emit(ProjectStructureEditEvent.Dismissed) // Placeholder
            hasChanges = false
        }
    }

    /**
     * 변경 사항을 취소하고 다이얼로그를 닫습니다.
     * UI 상태를 원래대로 되돌립니다.
     */
    fun onCancelChangesClicked() {
         _uiState.update { 
             it.copy(
                 draggableItems = it.originalDraggableItems, // Revert draggableItems
                 // Reset any other relevant states like dialogs
                 renameDialogState = null,
                 addChannelDialogTargetCategoryId = null,
                 contextMenuState = null,
                 deleteConfirmationInfo = null
             ) 
         }
         hasChanges = false // Reset this too, or manage more carefully
         viewModelScope.launch { _eventFlow.emit(ProjectStructureEditEvent.Dismissed) }
    }

    // Delete, Rename, Add operations will now modify the draggableItems list
    // and ensure the underlying Category/Channel objects are also updated.

    /**
     * 새 카테고리를 추가합니다. (DraggableItems 업데이트 필요)
     * @param categoryName 새로 추가할 카테고리의 이름.
     */
    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            // TODO: Implement actual logic to add a category to draggableItems
            // This involves creating a new Category object, then a new DraggableListItemData
            // and adding it to the _uiState.value.draggableItems list.
            // The order and depth will need to be determined.
            // For now, just log and potentially show a snackbar.
            Log.d("ViewModel", "Adding category: $categoryName")
            // Example:
            // val newCategory = Category(id = UUID.randomUUID().toString(), name = categoryName, order = _uiState.value.draggableItems.filterIsInstance<ProjectStructureDraggableItem.CategoryDraggable>().size)
            // val newCategoryItem = DraggableListItemData(
            // id = newCategory.id,
            // originalData = ProjectStructureDraggableItem.CategoryDraggable(newCategory),
            // depth = 0, // Or appropriate depth
            // parentId = null
            // )
            // _uiState.update { it.copy(draggableItems = it.draggableItems + newCategoryItem) }
            // setHasChanges()
            _eventFlow.emit(ProjectStructureEditEvent.ShowSnackbar("'$categoryName' 카테고리 추가 기능 구현 필요"))
        }
    }

    /**
     * 카테고리 삭제 요청 시 확인 다이얼로그를 표시하도록 UI 상태를 업데이트합니다.
     * @param categoryId 삭제할 카테고리 ID
     */
    fun requestRemoveCategory(categoryId: String) {
        val categoryToRemove = _uiState.value.categories.find { it.id == categoryId }
        categoryToRemove?.let {
            _uiState.update {
                it.copy(
                    deleteConfirmationInfo = DeleteConfirmationInfo(
                        title = "카테고리 삭제",
                        message = "\'${categoryToRemove.name}\' 카테고리를 정말 삭제하시겠습니까? 카테고리 내의 모든 채널도 함께 삭제됩니다.",
                        itemType = ChannelTypeForDeletion.CATEGORY, // DeleteItemType -> ChannelTypeForDeletion
                        categoryId = categoryId
                    )
                )
            }
        }
    }

    /**
     * 채널 삭제 요청 시 확인 다이얼로그를 표시하도록 UI 상태를 업데이트합니다.
     * @param categoryId 채널이 속한 카테고리 ID
     * @param channelId 삭제할 채널 ID
     */
    fun requestRemoveChannel(categoryId: String, channelId: String) {
        val category = _uiState.value.categories.find { it.id == categoryId }
        val channelToRemove = category?.channels?.find { it.id == channelId }

        if (category != null && channelToRemove != null) {
            _uiState.update {
                it.copy(
                    deleteConfirmationInfo = DeleteConfirmationInfo(
                        title = "채널 삭제",
                        message = "\'${channelToRemove.name}\' 채널을 정말 삭제하시겠습니까?",
                        itemType = ChannelTypeForDeletion.CHANNEL, // DeleteItemType -> ChannelTypeForDeletion
                        categoryId = categoryId,
                        channelId = channelId
                    )
                )
            }
        }
    }

    /**
     * 확인 다이얼로그에서 '확인'을 눌렀을 때 실제 삭제 로직을 실행합니다.
     */
    fun confirmDelete() {
        _uiState.value.deleteConfirmationInfo?.let { info ->
            when (info.itemType) {
                ChannelTypeForDeletion.CATEGORY -> deleteChannelService(info.categoryId, info.channelId!!)
                ChannelTypeForDeletion.CHANNEL -> deleteChannelService(info.categoryId, info.channelId!!)
            }
            _uiState.update { it.copy(deleteConfirmationInfo = null) }
        }
    }

    /**
     * 확인 다이얼로그에서 '취소'를 눌렀을 때 다이얼로그를 닫습니다.
     */
    fun cancelDelete() {
        _uiState.update { it.copy(deleteConfirmationInfo = null) }
    }

    /**
     * 카테고리 드래그 시작
     * @param index 드래그할 카테고리의 인덱스
     */
    fun startDragCategory(index: Int) {
        _uiState.update { 
            it.copy(
                isDragging = true,
                draggedCategoryIndex = index,
                draggedChannelInfo = null, // Clear other drag states
                draggedDirectChannelIndex = null
            )
        }
    }
    
    // 채널 드래그 시작
    fun startDragChannel(categoryId: String, channelIndex: Int, channelId: String) { // Added channelId
        _uiState.update { 
            it.copy(
                isDragging = true,
                draggedChannelInfo = DraggedChannelInfo(categoryId, channelIndex, channelId),
                draggedCategoryIndex = null, // Clear other drag states
                draggedDirectChannelIndex = null
            )
        }
    }
    
    // 드래그 종료
    fun endDrag() {
        _uiState.update { 
            it.copy(
                isDragging = false,
                draggedCategoryIndex = null,
                draggedChannelInfo = null,
                draggedDirectChannelIndex = null,
                draggedChannelId = null
            )
        }
    }
    
    // 카테고리 확장/축소 토글
    fun toggleCategoryExpand(categoryId: String) {
        _uiState.update { currentState ->
            val expandedMap = currentState.expandedCategories.toMutableMap()
            val isCurrentlyExpanded = expandedMap.getOrDefault(categoryId, true)
            expandedMap[categoryId] = !isCurrentlyExpanded
            
            currentState.copy(expandedCategories = expandedMap)
        }
    }
    
    // 카테고리 이름 변경 다이얼로그 열기
    fun openRenameCategoryDialog(categoryId: String) {
        _uiState.update { 
            it.copy(renameDialogState = RenameDialogState.Category(categoryId))
        }
    }
    
    // 채널 이름 변경 다이얼로그 열기
    fun openRenameChannelDialog(categoryId: String, channelId: String) {
        _uiState.update { 
            it.copy(renameDialogState = RenameDialogState.Channel(categoryId, channelId))
        }
    }
    
    // 이름 변경 다이얼로그 닫기
    fun closeRenameDialog() {
        _uiState.update { it.copy(renameDialogState = null) }
    }
    
    // 채널 추가 다이얼로그 열기
    fun openAddChannelDialog(categoryId: String) {
        _uiState.update { 
            it.copy(addChannelDialogTargetCategoryId = categoryId)
        }
    }
    
    // 채널 추가 다이얼로그 닫기
    fun closeAddChannelDialog() {
        _uiState.update { it.copy(addChannelDialogTargetCategoryId = null) }
    }
    
    // 컨텍스트 메뉴 열기
    fun openContextMenu(state: ContextMenuState) {
        _uiState.update { it.copy(contextMenuState = state) }
    }
    
    // 컨텍스트 메뉴 닫기
    fun closeContextMenu() {
        _uiState.update { it.copy(contextMenuState = null) }
    }

    /**
     * 이름 변경 다이얼로그에서 '확인'을 눌렀을 때 호출됩니다.
     * 현재 열려있는 이름 변경 대상(카테고리 또는 채널)의 이름을 변경합니다.
     * @param newName 변경할 새 이름.
     */
    fun handleRenameConfirm(newName: String) {
        val state = _uiState.value.renameDialogState ?: return
        viewModelScope.launch {
            when (state) {
                is RenameDialogState.Category -> renameCategory(state.categoryId, newName)
                is RenameDialogState.Channel -> renameChannel(state.categoryId, state.channelId, newName)
            }
            closeRenameDialog() // Close dialog after processing
        }
    }

    /**
     * 채널 추가 다이얼로그에서 '확인'을 눌렀을 때 호출됩니다.
     * 지정된 카테고리에 새 채널을 추가합니다.
     * @param channelName 추가할 채널의 이름.
     * @param channelMode 추가할 채널의 모드 (TEXT 또는 VOICE).
     */
    fun handleAddChannelConfirm(channelName: String, channelMode: ChannelMode) {
        val targetCategoryId = _uiState.value.addChannelDialogTargetCategoryId ?: return
        viewModelScope.launch {
            addChannel(targetCategoryId, channelName, channelMode)
            closeAddChannelDialog() // Close dialog after processing
        }
    }

    /**
     * 컨텍스트 메뉴에서 '이름 변경' 옵션을 선택했을 때 호출됩니다.
     * 선택된 아이템(카테고리 또는 채널)에 대한 이름 변경 다이얼로그를 엽니다.
     */
    fun handleContextMenuRename() {
        val menuState = _uiState.value.contextMenuState ?: return
        viewModelScope.launch {
            when (menuState) {
                is ContextMenuState.Category -> openRenameCategoryDialog(menuState.categoryId)
                is ContextMenuState.Channel -> openRenameChannelDialog(menuState.categoryId, menuState.channelId)
            }
            closeContextMenu() // Close menu after opening dialog
        }
    }

    /**
     * 컨텍스트 메뉴에서 '삭제' 옵션을 선택했을 때 호출됩니다.
     * 선택된 아이템(카테고리 또는 채널)에 대한 삭제 확인 요청을 시작합니다.
     */
    fun handleContextMenuDelete() {
        val menuState = _uiState.value.contextMenuState ?: return
        viewModelScope.launch {
            when (menuState) {
                is ContextMenuState.Category -> requestRemoveCategory(menuState.categoryId)
                is ContextMenuState.Channel -> requestRemoveChannel(menuState.categoryId, menuState.channelId)
            }
            closeContextMenu() // Close menu after initiating delete request
        }
    }

    /**
     * 카테고리 이름을 변경합니다. (DraggableItems 업데이트 필요)
     * @param categoryId 변경할 카테고리의 ID.
     * @param newName 카테고리의 새 이름.
     */
    fun renameCategory(categoryId: String, newName: String) {
        viewModelScope.launch {
            // TODO: Implement actual logic to rename a category in draggableItems
            // This involves finding the item, updating its name (and the name in originalData),
            // and then updating the _uiState.
            // For now, use the service and assume it might update a backend,
            // then we'd need to reflect that change in draggableItems.
            renameCategoryService(categoryId, newName).fold(
                onSuccess = { updatedCategory ->
                    // Find and update in draggableItems
                    val updatedItems = _uiState.value.draggableItems.map { item ->
                        if (item.id == categoryId && item.originalData is ProjectStructureDraggableItem.CategoryDraggable) {
                            item.copy(originalData = (item.originalData as ProjectStructureDraggableItem.CategoryDraggable).copy(category = updatedCategory))
                        } else {
                            item
                        }
                    }
                    _uiState.update { it.copy(draggableItems = updatedItems) }
                    // setHasChanges()
                    _eventFlow.emit(ProjectStructureEditEvent.ShowSnackbar("'${updatedCategory.name}' (으)로 이름 변경됨"))
                },
                onFailure = { e ->
                    _eventFlow.emit(ProjectStructureEditEvent.Error("카테고리 이름 변경 실패: ${e.message}"))
                }
            )
        }
    }

    /**
     * 채널 이름을 변경합니다. (DraggableItems 업데이트 필요)
     * @param categoryId 채널이 속한 카테고리의 ID (직속 채널의 경우 사용 방식 정의 필요).
     * @param channelId 변경할 채널의 ID.
     * @param newName 채널의 새 이름.
     */
    fun renameChannel(categoryId: String, channelId: String, newName: String) {
        viewModelScope.launch {
            renameChannelService(categoryId, channelId, newName).fold( // Assuming service only needs channelId and newName
                onSuccess = { updatedChannel ->
                    // Find and update in draggableItems
                    val updatedItems = _uiState.value.draggableItems.map { item ->
                        if (item.id == channelId && item.originalData is ProjectStructureDraggableItem.ChannelDraggable) {
                            item.copy(originalData = (item.originalData as ProjectStructureDraggableItem.ChannelDraggable).copy(channel = updatedChannel))
                        } else {
                            item
                        }
                    }
                    _uiState.update { it.copy(draggableItems = updatedItems) }
                    // setHasChanges()
                    _eventFlow.emit(ProjectStructureEditEvent.ShowSnackbar("'${updatedChannel.name}' (으)로 이름 변경됨"))
                },
                onFailure = { e ->
                    _eventFlow.emit(ProjectStructureEditEvent.Error("채널 이름 변경 실패: ${e.message}"))
                }
            )
        }
    }

    /**
     * 지정된 카테고리에 새 채널을 추가합니다. (DraggableItems 업데이트 필요)
     * @param categoryId 채널을 추가할 카테고리의 ID.
     * @param channelName 새로 추가할 채널의 이름.
     * @param channelMode 새로 추가할 채널의 모드 (TEXT 또는 VOICE).
     */
    fun addChannel(categoryId: String, channelName: String, channelMode: ChannelMode) {
        viewModelScope.launch {
            // TODO: Implement actual logic to add a channel to draggableItems under the given categoryId.
            // This involves creating a new Channel object, then a new DraggableListItemData,
            // finding the correct insertion point in the list, and updating depths.
            // For now, use the service and assume it might update a backend.
            val newChannel = Channel(
                id ="",
                name = channelName,
                type = ChannelType.PROJECT,
                description = "",
                projectSpecificData = ProjectSpecificData(projectId = _uiState.value.projectId, categoryId = categoryId, order = 0), // Order needs calculation
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant()
            )
            addChannelService(newChannel).fold(
                onSuccess = { addedChannel ->
                    // This is tricky: we need to insert this into draggableItems correctly.
                    // It should appear under its parent category, and its depth should be parentDepth + 1.
                    // The order within the category also matters.
                    // For now, just log and show a snackbar. A full implementation is complex.
                    Log.d("ViewModel", "Channel added: ${addedChannel.name} to category $categoryId. UI update needed.")
                    // Example placeholder for adding to list (very simplified, won't place correctly):
                    // val newChannelItem = DraggableListItemData(
                    // id = addedChannel.id,
                    // originalData = ProjectStructureDraggableItem.ChannelDraggable(addedChannel, categoryId),
                    // depth = 1, // This needs to be dynamic
                    // parentId = categoryId
                    // )
                    // _uiState.update { it.copy(draggableItems = it.draggableItems + newChannelItem) }
                    // setHasChanges()
                    _eventFlow.emit(ProjectStructureEditEvent.ShowSnackbar("'${addedChannel.name}' 채널 추가됨 (UI 업데이트 필요)"))
                },
                onFailure = { e ->
                    _eventFlow.emit(ProjectStructureEditEvent.Error("채널 추가 실패: ${e.message}"))
                }
            )
        }
    }
}

/**
 * 프로젝트 구조 편집 화면의 UI 상태 (DraggableList 통합)
 */
data class ProjectStructureEditUiState(
    val projectId: String = "",
    val draggableItems: List<DraggableListItemData<ProjectStructureDraggableItem>> = emptyList(),
    val originalDraggableItems: List<DraggableListItemData<ProjectStructureDraggableItem>> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // Drag states from previous ViewModel are now managed by DraggableListState, so remove them here
    val isDragging: Boolean = false,
    val draggedCategoryIndex: Int? = null, 
    val draggedChannelInfo: DraggedChannelInfo? = null,
    val draggedDirectChannelIndex: Int? = null,
    val draggedChannelId: String? = null,
    val expandedCategories: Map<String, Boolean> = emptyMap(), // This might need to be adapted for draggableItems if categories can still expand/collapse
    val renameDialogState: RenameDialogState? = null,
    val addChannelDialogTargetCategoryId: String? = null,
    val contextMenuState: ContextMenuState? = null,
    val deleteConfirmationInfo: DeleteConfirmationInfo? = null,

    // Deprecated, to be removed once draggableItems is fully used
    val categories: List<Category> = emptyList(), 
    val originalCategories: List<Category> = emptyList(), 
    val directChannels: List<Channel> = emptyList(),
    val originalDirectChannels: List<Channel> = emptyList()
)

/**
 * 프로젝트 구조 편집 화면의 이벤트
 */
sealed class ProjectStructureEditEvent {
    /** 변경 사항이 저장됨 */
    object SavedChanges : ProjectStructureEditEvent()
    
    /** 다이얼로그가 닫힘 */
    object Dismissed : ProjectStructureEditEvent()
    
    /** 에러 발생 */
    data class Error(val message: String) : ProjectStructureEditEvent()
    
    /** 스낵바 표시 */
    data class ShowSnackbar(val message: String) : ProjectStructureEditEvent()
    
    /** 카테고리 드래그 시작 */
    data class StartDragCategory(val index: Int) : ProjectStructureEditEvent()
    
    /** 채널 드래그 시작 */
    data class StartDragChannel(val categoryId: String, val channelIndex: Int) : ProjectStructureEditEvent()
    
    /** 드래그 종료 */
    object EndDrag : ProjectStructureEditEvent()
    
    /** 카테고리 확장/축소 토글 */
    data class ToggleCategoryExpand(val categoryId: String) : ProjectStructureEditEvent()
    
    /** 카테고리 이름 변경 다이얼로그 열기 */
    data class OpenRenameCategoryDialog(val categoryId: String) : ProjectStructureEditEvent()
    
    /** 채널 이름 변경 다이얼로그 열기 */
    data class OpenRenameChannelDialog(val categoryId: String, val channelId: String) : ProjectStructureEditEvent()
    
    /** 이름 변경 다이얼로그 닫기 */
    object CloseRenameDialog : ProjectStructureEditEvent()
    
    /** 채널 추가 다이얼로그 열기 */
    data class OpenAddChannelDialog(val categoryId: String) : ProjectStructureEditEvent()
    
    /** 채널 추가 다이얼로그 닫기 */
    object CloseAddChannelDialog : ProjectStructureEditEvent()
    
    /** 컨텍스트 메뉴 열기 */
    data class OpenContextMenu(val state: ContextMenuState) : ProjectStructureEditEvent()
    
    /** 컨텍스트 메뉴 닫기 */
    object CloseContextMenu : ProjectStructureEditEvent()
}

/**
 * 드래그 중인 채널 정보
 */
data class DraggedChannelInfo(
    val categoryId: String,
    val channelIndex: Int,
    val channelId: String // Added channelId
)

/**
 * 이름 변경 대화상자 상태
 */
sealed class RenameDialogState {
    data class Category(val categoryId: String) : RenameDialogState()
    data class Channel(val categoryId: String, val channelId: String) : RenameDialogState()
}

/**
 * 컨텍스트 메뉴 상태
 */
sealed class ContextMenuState {
    data class Category(val categoryId: String, val position: androidx.compose.ui.geometry.Offset) : ContextMenuState()
    data class Channel(val categoryId: String, val channelId: String, val position: androidx.compose.ui.geometry.Offset) : ContextMenuState()
} 