package com.example.core_ui.dialogs.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.util.DateTimeUtil
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
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = getProjectStructureUseCase(projectId)
                result.fold(
                    onSuccess = { structure ->
                        // 도메인 모델을 직접 사용
                        _uiState.update {
                            it.copy(
                                projectId = projectId,
                                categories = structure.categories,
                                originalCategories = structure.categories, // 원본도 도메인 모델로 저장
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "프로젝트 구조를 불러오지 못했습니다: ${e.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "프로젝트 구조를 불러오지 못했습니다: ${e.message}"
                    )
                }
            }
        }
    }

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
        fromCategoryId: String,
        fromIndex: Int,
        toCategoryId: String,
        toIndex: Int
    ) {
        if (fromCategoryId == toCategoryId && fromIndex == toIndex) return
        
        val categories = _uiState.value.categories.toMutableList()
        val now = DateTimeUtil.nowInstant()
        
        val sourceCategoryIndex = categories.indexOfFirst { it.id == fromCategoryId }
        val targetCategoryIndex = categories.indexOfFirst { it.id == toCategoryId }
        
        if (sourceCategoryIndex == -1 || targetCategoryIndex == -1) return
        
        val sourceCategory = categories[sourceCategoryIndex]
        var movedChannel: Channel? = null
        
        // 1. 원본 카테고리에서 채널 제거 및 order 업데이트
        val updatedSourceChannels = sourceCategory.channels.toMutableList()
        movedChannel = updatedSourceChannels.removeAt(fromIndex)
        categories[sourceCategoryIndex] = sourceCategory.copy(
            channels = updatedSourceChannels.mapIndexed { index, channel ->
                channel.copy(
                    projectSpecificData = channel.projectSpecificData?.copy(order = index),
                    updatedAt = now
                )
            },
            updatedAt = now
        )
        
        if (movedChannel == null) return

        // 2. 대상 카테고리에 채널 추가 및 order 업데이트
        val targetCategory = categories[targetCategoryIndex]
        val updatedTargetChannels = targetCategory.channels.toMutableList()
        
        // 채널의 categoryId 및 projectSpecificData 업데이트
        val finalMovedChannel = movedChannel.copy(
            projectSpecificData = movedChannel.projectSpecificData?.copy(categoryId = toCategoryId),
            updatedAt = now
        )
        updatedTargetChannels.add(toIndex, finalMovedChannel)
        
        categories[targetCategoryIndex] = targetCategory.copy(
            channels = updatedTargetChannels.mapIndexed { index, channel ->
                channel.copy(
                    projectSpecificData = channel.projectSpecificData?.copy(order = index),
                    updatedAt = now
                )
            },
            updatedAt = now
        )
        
        _uiState.update { it.copy(categories = categories) }
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
     * 변경 사항을 저장합니다.
     */
    fun saveChanges() {
        if (!hasChanges) {
            viewModelScope.launch {
                _eventFlow.emit(ProjectStructureEditEvent.Dismissed)
            }
            return
        }
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // UI 상태의 카테고리 목록을 사용하여 ProjectStructure 객체 생성
                val now = DateTimeUtil.nowInstant()
                
                // 디버깅: 채널 ID 존재 여부 로깅
                _uiState.value.categories.forEach { category ->
                    category.channels.forEach { channel ->
                        Log.d("ProjectStructureEditVM", "채널 확인: id=${channel.id}, name=${channel.name}, 빈 ID=${channel.id.isBlank()}")
                    }
                }
                
                val finalCategories = _uiState.value.categories.mapIndexed { catIndex, category ->
                    val updatedCategory = category.copy(
                        order = catIndex, // 최종 순서 반영
                        updatedAt = if (category.updatedAt != null && category.updatedAt!! < now) now else category.updatedAt, // 변경된 경우 시간 업데이트
                        channels = category.channels.mapIndexed { chIndex, channel ->
                            channel.copy(
                                projectSpecificData = channel.projectSpecificData?.copy(order = chIndex), // 최종 순서 반영
                                updatedAt = if (channel.updatedAt != null && channel.updatedAt < now) now else channel.updatedAt // 변경된 경우 시간 업데이트
                            )
                        }
                    )
                    updatedCategory
                }
                
                val domainProjectStructure = ProjectStructure(
                    categories = finalCategories,
                    directChannels = emptyList() // 현재 UI에서 직속 채널은 다루지 않음
                )
                
                // UpdateProjectStructureUseCase를 사용하여 변경 사항 저장
                updateProjectStructureUseCase(_uiState.value.projectId, domainProjectStructure)
                    .onSuccess {
                        _uiState.update { it.copy(isLoading = false) }
                        hasChanges = false // 저장 성공 후 변경 사항 없음으로 표시
                        _eventFlow.emit(ProjectStructureEditEvent.SavedChanges)
                    }
                    .onFailure { error ->
                        // 더 자세한 오류 정보 로깅
                        val errorMessage = "변경 사항을 저장하지 못했습니다: ${error.message}"
                        val causeMessage = error.cause?.message ?: "원인 없음"
                        Log.e("ProjectStructureEditVM", "$errorMessage\n원인: $causeMessage", error)
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = errorMessage
                            )
                        }
                        _eventFlow.emit(ProjectStructureEditEvent.Error(errorMessage))
                    }
            } catch (e: Exception) {
                Log.e("ProjectStructureEditVM", "변경 사항 저장 중 예외 발생", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "변경 사항을 저장하지 못했습니다: ${e.message}"
                    )
                }
                _eventFlow.emit(ProjectStructureEditEvent.Error("변경 사항을 저장하지 못했습니다: ${e.message}"))
            }
        }
    }

    /**
     * 변경 사항을 취소합니다.
     */
    fun cancelChanges() {
        if (hasChanges) {
            // 변경 사항이 있는 경우, 원래 데이터로 복원
            // originalCategories도 도메인 모델 리스트이므로 직접 사용
            _uiState.update { it.copy(categories = it.originalCategories) }
            hasChanges = false
        }
        
        viewModelScope.launch {
            _eventFlow.emit(ProjectStructureEditEvent.Dismissed)
        }
    }

    // 카테고리 드래그 시작
    fun startDragCategory(index: Int) {
        _uiState.update { 
            it.copy(
                isDragging = true,
                draggedCategoryIndex = index
            )
        }
    }
    
    // 채널 드래그 시작
    fun startDragChannel(categoryId: String, channelIndex: Int) {
        _uiState.update { 
            it.copy(
                isDragging = true,
                draggedChannelInfo = DraggedChannelInfo(categoryId, channelIndex)
            )
        }
    }
    
    // 드래그 종료
    fun endDrag() {
        _uiState.update { 
            it.copy(
                isDragging = false,
                draggedCategoryIndex = null,
                draggedChannelInfo = null
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
}

/**
 * 프로젝트 구조 편집 화면의 UI 상태
 */
data class ProjectStructureEditUiState(
    val projectId: String = "",
    val categories: List<Category> = emptyList(), // CategoryItem -> Category
    val originalCategories: List<Category> = emptyList(), // CategoryItem -> Category
    val isLoading: Boolean = false,
    val error: String? = null,
    // 추가된 UI 상태
    val isDragging: Boolean = false,
    val draggedCategoryIndex: Int? = null,
    val draggedChannelInfo: DraggedChannelInfo? = null,
    val expandedCategories: Map<String, Boolean> = emptyMap(),
    val renameDialogState: RenameDialogState? = null,
    val addChannelDialogTargetCategoryId: String? = null,
    val contextMenuState: ContextMenuState? = null
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
    val channelIndex: Int
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