package com.example.core_ui.dialogs.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.util.DateTimeUtil
import com.example.core_ui.components.draggablelist.DraggableListItemData
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
    private val updateProjectStructureUseCase: UpdateProjectStructureUseCase
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
                        val newDraggableItems = convertProjectStructureToDraggableItems(structure)
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
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "프로젝트 구조를 불러오지 못했습니다: ${e.message}")
                }
            }
        }
    }

    private fun convertProjectStructureToDraggableItems(structure: ProjectStructure): List<DraggableListItemData<ProjectStructureDraggableItem>> {
        val draggableItems = mutableListOf<DraggableListItemData<ProjectStructureDraggableItem>>()
        var orderInList = 0

        // Add categories and their channels
        structure.categories.sortedBy { it.order }.forEach { category ->
            draggableItems.add(
                DraggableListItemData(
                    id = category.id,
                    originalData = ProjectStructureDraggableItem.CategoryDraggable(category),
                    depth = 0,
                    parentId = null,
                    canAcceptChildren = true,
                    maxRelativeChildDepth = 1 // Categories can have channels (depth 1 relative to category)
                )
            )
            orderInList++
            category.channels.sortedBy { it.projectSpecificData?.order ?: 0 }.forEach { channel ->
                draggableItems.add(
                    DraggableListItemData(
                        id = channel.id,
                        originalData = ProjectStructureDraggableItem.ChannelDraggable(channel, category.id),
                        depth = 1,
                        parentId = category.id,
                        canAcceptChildren = false,
                        maxRelativeChildDepth = 0
                    )
                )
                orderInList++
            }
        }

        // Add direct channels (as depth 0 items, distinct from categories)
        structure.directChannels.forEach { channel -> // Assuming direct channels don't have an explicit order field, use list order
            draggableItems.add(
                DraggableListItemData(
                    id = channel.id,
                    originalData = ProjectStructureDraggableItem.ChannelDraggable(channel, null),
                    depth = 0, // Direct channels are at root level
                    parentId = null, // Or a special root ID if needed to distinguish from categories
                    canAcceptChildren = false,
                    maxRelativeChildDepth = 0
                )
            )
            orderInList++
        }
        return draggableItems
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

    fun onSaveChangesClicked() { // Renamed from saveChanges to avoid conflict if we keep old one for a bit
        if (!calculateHasChanges()) {
            viewModelScope.launch { _eventFlow.emit(ProjectStructureEditEvent.Dismissed) }
            return
        }
        // ... (conversion to ProjectStructure and calling use case)
    }

    fun onCancelChangesClicked() {
         _uiState.update { 
             it.copy(
                 draggableItems = it.originalDraggableItems // Revert draggableItems
             ) 
         }
         hasChanges = false // Reset this too, or manage more carefully
         viewModelScope.launch { _eventFlow.emit(ProjectStructureEditEvent.Dismissed) }
    }

    // Delete, Rename, Add operations will now modify the draggableItems list
    // and ensure the underlying Category/Channel objects are also updated.

    /**
     * 카테고리를 추가합니다.
     */
    fun addCategory() {
        val newCategoryId = UUID.randomUUID().toString()
        val now = DateTimeUtil.nowInstant()
        val newCategory = Category(
            id = newCategoryId,
            projectId = _uiState.value.projectId,
            name = "새 카테고리",
            order = _uiState.value.categories.size, // 새 카테고리는 마지막 순서
            channels = emptyList(),
            createdAt = now,
            updatedAt = now,
            // createdBy, updatedBy는 현재 ViewModel에서 알 수 없으므로 null 또는 기본값 처리
            createdBy = null, 
            updatedBy = null
        )
        
        val updatedCategories = _uiState.value.categories.toMutableList().apply {
            add(newCategory)
        }
        
        _uiState.update { it.copy(categories = updatedCategories) }
        hasChanges = true
    }

    /**
     * 특정 카테고리에 채널을 추가합니다.
     * @param categoryId 채널을 추가할 카테고리 ID
     * @param channelName 추가할 채널 이름
     * @param channelMode 채널 유형 (기본값: TEXT)
     */
    fun addChannel(categoryId: String, channelName: String, channelMode: ChannelMode = ChannelMode.TEXT) {
        val newChannelId = UUID.randomUUID().toString()
        val now = DateTimeUtil.nowInstant()
        
        val updatedCategories = _uiState.value.categories.map { category ->
            if (category.id == categoryId) {
                val newChannel = Channel(
                    id = newChannelId,
                    name = channelName,
                    description = null, // 기본값
                    type = ChannelType.PROJECT, // 채널은 카테고리 내에 속함
                    projectSpecificData = ProjectSpecificData(
                        projectId = _uiState.value.projectId,
                        categoryId = categoryId,
                        order = category.channels.size, // 새 채널은 마지막 순서
                        channelMode = channelMode 
                    ),
                    dmSpecificData = null, // 프로젝트 채널이므로 null
                    lastMessagePreview = null,
                    lastMessageTimestamp = null,
                    createdAt = now,
                    updatedAt = now,
                    createdBy = null // createdBy는 현재 ViewModel에서 알 수 없으므로 null
                )
                val updatedChannels = category.channels.toMutableList().apply {
                    add(newChannel)
                }
                category.copy(channels = updatedChannels, updatedAt = now) // 카테고리 업데이트 시간 변경
            } else {
                category
            }
        }
        
        _uiState.update { it.copy(categories = updatedCategories) }
        hasChanges = true
    }

    /**
     * 카테고리 순서를 변경합니다.
     * @param fromIndex 이동할 카테고리의 현재 인덱스
     * @param toIndex 이동할 목표 인덱스
     */
    fun moveCategory(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        
        val categories = _uiState.value.categories.toMutableList()
        val movedCategory = categories.removeAt(fromIndex)
        categories.add(toIndex, movedCategory)
        
        // 순서 변경에 따른 order 필드 업데이트
        val now = DateTimeUtil.nowInstant()
        val updatedCategoriesWithOrder = categories.mapIndexed { index, category ->
            category.copy(order = index, updatedAt = now)
        }
        
        _uiState.update { it.copy(categories = updatedCategoriesWithOrder) }
        hasChanges = true
    }

    /**
     * 채널 순서를 변경하거나 다른 카테고리로 이동합니다.
     * @param fromCategoryId 원본 카테고리 ID
     * @param fromIndex 이동할 채널의 현재 인덱스
     * @param toCategoryId 대상 카테고리 ID
     * @param toIndex 이동할 목표 인덱스
     */
    fun moveChannel(
        fromCategoryId: String? = null, // Null if dragging from direct channels
        fromChannelId: String,
        fromIndex: Int, // Index within source (category or direct list)
        toCategoryId: String? = null, // Null if dropping to direct channels
        toIndex: Int // Index within target (category or direct list)
    ) {
        if (fromCategoryId == toCategoryId && fromIndex == toIndex && fromCategoryId != null) return // No change within the same category
        if (fromCategoryId == null && toCategoryId == null && fromIndex == toIndex) return // No change within direct channels

        val now = DateTimeUtil.nowInstant()
        var currentCategories = _uiState.value.categories.toMutableList()
        var currentDirectChannels = _uiState.value.directChannels.toMutableList()
        var movedChannel: Channel? = null

        // 1. Remove channel from source
        if (fromCategoryId != null) {
            val sourceCategoryIndex = currentCategories.indexOfFirst { it.id == fromCategoryId }
            if (sourceCategoryIndex == -1) return
            val sourceCategory = currentCategories[sourceCategoryIndex]
            val mutableSourceChannels = sourceCategory.channels.toMutableList()
            movedChannel = mutableSourceChannels.find { it.id == fromChannelId } ?: return
            mutableSourceChannels.remove(movedChannel)
            currentCategories[sourceCategoryIndex] = sourceCategory.copy(
                channels = mutableSourceChannels.mapIndexed { idx, ch -> ch.copy(projectSpecificData = ch.projectSpecificData?.copy(order = idx), updatedAt = now) },
                updatedAt = now
            )
        } else { // Source is direct channels
            movedChannel = currentDirectChannels.find { it.id == fromChannelId } ?: return
            currentDirectChannels.remove(movedChannel)
            // Re-order direct channels is done after adding to target, or at the end if target is also direct
        }

        // 2. Add channel to target
        if (toCategoryId != null) {
            val targetCategoryIndex = currentCategories.indexOfFirst { it.id == toCategoryId }
            if (targetCategoryIndex == -1) return // Should not happen if UI is correct
            val targetCategory = currentCategories[targetCategoryIndex]
            val mutableTargetChannels = targetCategory.channels.toMutableList()
            val updatedMovedChannel = movedChannel.copy(
                projectSpecificData = movedChannel.projectSpecificData?.copy(categoryId = toCategoryId, order = toIndex),
                updatedAt = now,
                type = ChannelType.PROJECT // Ensure type is PROJECT when moved to a category
            )
            mutableTargetChannels.add(toIndex.coerceIn(0, mutableTargetChannels.size), updatedMovedChannel)
            currentCategories[targetCategoryIndex] = targetCategory.copy(
                channels = mutableTargetChannels.mapIndexed { idx, ch -> ch.copy(projectSpecificData = ch.projectSpecificData?.copy(order = idx), updatedAt = now) },
                updatedAt = now
            )
        } else { // Target is direct channels
            val updatedMovedChannel = movedChannel.copy(
                projectSpecificData = null, // No categoryId or order within category for direct channels
                dmSpecificData = null, // Assuming direct channels in project structure are not DMs
                type = ChannelType.PROJECT_DIRECT, // Or a new type to distinguish if needed
                updatedAt = now
            )
            currentDirectChannels.add(toIndex.coerceIn(0, currentDirectChannels.size), updatedMovedChannel)
            // Re-order direct channels after add
            currentDirectChannels = currentDirectChannels.mapIndexed { idx, ch -> 
                ch.copy(updatedAt = if(ch.id == updatedMovedChannel.id) now else ch.updatedAt) // Update timestamp for moved, map order if needed by a specific field
            }.toMutableList()
        }
        
        // If source was direct channels and target is also direct channels, re-order direct channels now
        if (fromCategoryId == null && toCategoryId == null) {
             currentDirectChannels = _uiState.value.directChannels.toMutableList()
             val channelToMove = currentDirectChannels.find { it.id == fromChannelId } ?: return
             currentDirectChannels.remove(channelToMove)
             currentDirectChannels.add(toIndex.coerceIn(0, currentDirectChannels.size), channelToMove.copy(updatedAt = now))
             currentDirectChannels = currentDirectChannels.mapIndexed { idx, ch -> 
                // Potentially update an 'order' field if direct channels have one
                ch.copy(updatedAt = if (ch.updatedAt != now) ch.updatedAt else now) 
            }.toMutableList()
        }

        _uiState.update { it.copy(categories = currentCategories, directChannels = currentDirectChannels) }
        hasChanges = true
    }

    /**
     * 카테고리 이름을 변경합니다.
     * @param categoryId 이름을 변경할 카테고리 ID
     * @param newName 새 이름
     */
    fun renameCategory(categoryId: String, newName: String) {
        if (newName.isBlank()) return
        val now = DateTimeUtil.nowInstant()
        val updatedCategories = _uiState.value.categories.map { category ->
            if (category.id == categoryId) {
                category.copy(name = newName, updatedAt = now)
            } else {
                category
            }
        }
        
        _uiState.update { it.copy(categories = updatedCategories) }
        hasChanges = true
    }

    /**
     * 채널 이름을 변경합니다.
     * @param categoryId 채널이 속한 카테고리 ID
     * @param channelId 이름을 변경할 채널 ID
     * @param newName 새 이름
     */
    fun renameChannel(categoryId: String, channelId: String, newName: String) {
        if (newName.isBlank()) return
        val now = DateTimeUtil.nowInstant()
        val updatedCategories = _uiState.value.categories.map { category ->
            if (category.id == categoryId) {
                val updatedChannels = category.channels.map { channel ->
                    if (channel.id == channelId) {
                        channel.copy(name = newName, updatedAt = now)
                    } else {
                        channel
                    }
                }
                category.copy(channels = updatedChannels, updatedAt = now)
            } else {
                category
            }
        }
        
        _uiState.update { it.copy(categories = updatedCategories) }
        hasChanges = true
    }

    /**
     * 카테고리를 삭제합니다.
     * @param categoryId 삭제할 카테고리 ID
     */
    fun deleteCategory(categoryId: String) {
        val now = DateTimeUtil.nowInstant()
        // 삭제 후 남은 카테고리들의 순서를 재정렬
        val updatedCategories = _uiState.value.categories
            .filter { it.id != categoryId }
            .mapIndexed { index, category ->
                category.copy(order = index, updatedAt = now)
            }
        
        _uiState.update { it.copy(categories = updatedCategories) }
        hasChanges = true
    }

    /**
     * 채널을 삭제합니다.
     * @param categoryId 채널이 속한 카테고리 ID
     * @param channelId 삭제할 채널 ID
     */
    fun deleteChannel(categoryId: String, channelId: String) {
        val now = DateTimeUtil.nowInstant()
        val updatedCategories = _uiState.value.categories.map { category ->
            if (category.id == categoryId) {
                // 채널 삭제 후 남은 채널들의 순서를 재정렬
                val updatedChannels = category.channels
                    .filter { it.id != channelId }
                    .mapIndexed { index, channel ->
                        channel.copy(
                            projectSpecificData = channel.projectSpecificData?.copy(order = index),
                            updatedAt = now
                        )
                    }
                category.copy(channels = updatedChannels, updatedAt = now)
            } else {
                category
            }
        }
        
        _uiState.update { it.copy(categories = updatedCategories) }
        hasChanges = true
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
                ChannelTypeForDeletion.CATEGORY -> deleteCategory(info.categoryId)
                ChannelTypeForDeletion.CHANNEL -> deleteChannel(info.categoryId, info.channelId!!)
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
    // val isDragging: Boolean = false,
    // val draggedCategoryIndex: Int? = null, 
    // val draggedChannelInfo: DraggedChannelInfo? = null,
    // val draggedDirectChannelIndex: Int? = null,
    // val draggedChannelId: String? = null,
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