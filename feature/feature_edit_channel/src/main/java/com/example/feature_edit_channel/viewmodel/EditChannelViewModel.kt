package com.example.feature_edit_channel.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.base.Category
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.provider.project.ProjectStructureUseCaseProvider
import com.example.domain.provider.project.ProjectChannelUseCaseProvider
import com.example.core_common.result.CustomResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ChannelType enum은 CreateChannelViewModel과 공유하거나 별도 파일로 분리 가능 -> domain/model/ChannelType 으로 이동했으므로 제거

// --- UI 상태 ---
data class EditChannelUiState(
    val channelId: String = "",
    val currentChannelName: String = "",
    val originalChannelName: String = "", // 변경 여부 확인용
    val currentChannelMode: ProjectChannelType = ProjectChannelType.MESSAGES,
    val originalChannelMode: ProjectChannelType = ProjectChannelType.MESSAGES,
    val currentCategoryId: String = "",
    val originalCategoryId: String = "",
    val currentOrder: Double = 0.0,
    val originalOrder: Double = 0.0,
    val availableCategories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingCategories: Boolean = false,
    val error: String? = null,
    val updateSuccess: Boolean = false, // 업데이트 성공 시 네비게이션 트리거
    val deleteSuccess: Boolean = false // 삭제 성공 시 네비게이션 트리거
)

// --- 이벤트 ---
sealed class EditChannelEvent {
    object NavigateBack : EditChannelEvent()
    data class ShowSnackbar(val message: String) : EditChannelEvent()
    object ClearFocus : EditChannelEvent()
    object ShowDeleteConfirmation : EditChannelEvent()
}


@HiltViewModel
class EditChannelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectStructureUseCaseProvider: ProjectStructureUseCaseProvider,
    private val projectChannelUseCaseProvider: ProjectChannelUseCaseProvider
) : ViewModel() {

    private val projectId: String = savedStateHandle.getRequiredString(RouteArgs.PROJECT_ID)
    private val categoryId: String = savedStateHandle.getRequiredString(RouteArgs.CATEGORY_ID)
    private val channelId: String = savedStateHandle.getRequiredString(RouteArgs.CHANNEL_ID)

    private val _uiState = MutableStateFlow(EditChannelUiState(channelId = channelId, isLoading = true))
    val uiState: StateFlow<EditChannelUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditChannelEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // UseCase들을 Provider로부터 생성
    private val structureUseCases by lazy {
        projectStructureUseCaseProvider.createForProject(DocumentId(projectId))
    }
    
    private val channelUseCases by lazy {
        projectChannelUseCaseProvider.createForProject(DocumentId(projectId))
    }

    init {
        loadChannelDetails()
        loadCategories()
    }

    /**
     * 프로젝트의 모든 카테고리 목록 로드
     */
    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategories = true) }
            
            structureUseCases.getProjectAllCategoriesUseCase(DocumentId(projectId))
                .catch { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoadingCategories = false,
                            error = "카테고리 목록을 불러오지 못했습니다: ${exception.message}"
                        ) 
                    }
                }
                .collect { result ->
                    when (result) {
                        is CustomResult.Success -> {
                            _uiState.update { 
                                it.copy(
                                    isLoadingCategories = false,
                                    availableCategories = result.data
                                ) 
                            }
                        }
                        is CustomResult.Failure -> {
                            _uiState.update { 
                                it.copy(
                                    isLoadingCategories = false,
                                    error = "카테고리 목록을 불러오지 못했습니다: ${result.error.message}"
                                ) 
                            }
                        }
                        is CustomResult.Loading -> {
                            _uiState.update { it.copy(isLoadingCategories = true) }
                        }
                        else -> {
                            // Initial, Progress 상태 처리
                        }
                    }
                }
        }
    }

    /**
     * 초기 채널 정보 로드
     */
    private fun loadChannelDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading details for channel $channelId")
            
            // --- TODO: 실제 채널 정보 로드 (channelUseCases.getProjectChannelUseCase 사용) ---
            delay(500)
            val success = true
            val currentName = "기존 채널 이름 $channelId" // 임시
            val currentType = ProjectChannelType.MESSAGES
            val currentOrder = channelId.hashCode().toDouble() % 100.0 // 임시 순서값
            // -------------------------------------------------------------
            
            if (success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentChannelName = currentName,
                        originalChannelName = currentName,
                        currentChannelMode = currentType,
                        originalChannelMode = currentType,
                        currentCategoryId = categoryId,
                        originalCategoryId = categoryId,
                        currentOrder = currentOrder,
                        originalOrder = currentOrder
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "채널 정보를 불러오지 못했습니다."
                    )
                }
            }
        }
    }

    /**
     * 채널 이름 입력 변경 시 호출
     */
    fun onChannelNameChange(newName: String) {
        _uiState.update {
            it.copy(currentChannelName = newName, error = null)
        }
    }

    /**
     * 채널 유형 선택 시 호출
     */
    fun onChannelTypeSelected(newType: ProjectChannelType) {
        _uiState.update { it.copy(currentChannelMode = newType) }
    }

    /**
     * 카테고리 선택 시 호출
     */
    fun onCategorySelected(categoryId: String) {
        _uiState.update { it.copy(currentCategoryId = categoryId) }
    }

    /**
     * 순서 변경 시 호출
     */
    fun onOrderChange(newOrder: String) {
        val orderValue = newOrder.toDoubleOrNull()
        if (orderValue != null) {
            _uiState.update { it.copy(currentOrder = orderValue, error = null) }
        } else {
            _uiState.update { it.copy(error = "올바른 숫자를 입력해주세요.") }
        }
    }

    /**
     * 수정 완료 버튼 클릭 시 호출
     */
    fun updateChannel() {
        val currentState = _uiState.value
        val newName = currentState.currentChannelName.trim()
        val newType = currentState.currentChannelMode
        val newCategoryId = currentState.currentCategoryId
        val newOrder = currentState.currentOrder

        // 이름 유효성 검사
        if (newName.isBlank()) {
            _uiState.update { it.copy(error = "채널 이름을 입력해주세요.") }
            return
        }

        // 카테고리 선택 검사
        if (newCategoryId.isBlank()) {
            _uiState.update { it.copy(error = "카테고리를 선택해주세요.") }
            return
        }

        // 변경 여부 확인
        if (newName == currentState.originalChannelName && 
            newType == currentState.originalChannelMode &&
            newCategoryId == currentState.originalCategoryId &&
            newOrder == currentState.originalOrder) {
            viewModelScope.launch {
                _eventFlow.emit(EditChannelEvent.ShowSnackbar("변경된 내용이 없습니다."))
                _eventFlow.emit(EditChannelEvent.NavigateBack)
            }
            return
        }

        if (currentState.isLoading) return // 로딩 중 중복 실행 방지

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            _eventFlow.emit(EditChannelEvent.ClearFocus)
            println("ViewModel: Updating channel $channelId to '$newName' (${newType}) in category $newCategoryId with order $newOrder")

            // --- TODO: 실제 채널 업데이트 로직 (channelUseCases.updateProjectChannelUseCase 사용) ---
            delay(1000)
            val success = true
            // --------------------------------------------------------------

            if (success) {
                _eventFlow.emit(EditChannelEvent.ShowSnackbar("채널 정보가 수정되었습니다."))
                _uiState.update { it.copy(isLoading = false, updateSuccess = true) }
            } else {
                val errorMessage = "채널 수정 실패"
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    /**
     * 삭제 버튼 클릭 시 호출 (TopAppBar Action)
     */
    fun onDeleteClick() {
        viewModelScope.launch {
            _eventFlow.emit(EditChannelEvent.ShowDeleteConfirmation)
        }
    }

    /**
     * 삭제 확인 다이얼로그에서 '삭제' 버튼 클릭 시 호출
     */
    fun confirmDelete() {
        if (_uiState.value.isLoading) return // 로딩 중 중복 실행 방지

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Deleting channel $channelId")

            // --- TODO: 실제 채널 삭제 로직 (channelUseCases.deleteChannelUseCase 사용) ---
            delay(1000)
            val success = true
            // ---------------------------------------------------------

            if (success) {
                _eventFlow.emit(EditChannelEvent.ShowSnackbar("채널이 삭제되었습니다."))
                _uiState.update { it.copy(isLoading = false, deleteSuccess = true) }
            } else {
                val errorMessage = "채널 삭제 실패"
                _eventFlow.emit(EditChannelEvent.ShowSnackbar(errorMessage))
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}