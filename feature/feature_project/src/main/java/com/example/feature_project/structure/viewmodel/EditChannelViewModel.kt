package com.example.feature_project.structure.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.extension.getRequiredString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.domain.model.ChannelMode

// ChannelType enum은 CreateChannelViewModel과 공유하거나 별도 파일로 분리 가능 -> domain/model/ChannelType 으로 이동했으므로 제거

// --- UI 상태 ---
data class EditChannelUiState(
    val channelId: String = "",
    val currentChannelName: String = "",
    val originalChannelName: String = "", // 변경 여부 확인용
    val currentChannelMode: ChannelMode = ChannelMode.TEXT,
    val originalChannelMode: ChannelMode = ChannelMode.TEXT,
    val isLoading: Boolean = false,
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
    // TODO: private val repository: ProjectStructureRepository
) : ViewModel() {

    private val projectId: String = savedStateHandle.getRequiredString(AppRoutes.Project.ARG_PROJECT_ID)
    private val categoryId: String = savedStateHandle.getRequiredString(AppRoutes.Project.ARG_CATEGORY_ID)
    private val channelId: String = savedStateHandle.getRequiredString(AppRoutes.Project.ARG_CHANNEL_ID)

    private val _uiState = MutableStateFlow(EditChannelUiState(channelId = channelId, isLoading = true))
    val uiState: StateFlow<EditChannelUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditChannelEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadChannelDetails()
    }

    /**
     * 초기 채널 정보 로드
     */
    private fun loadChannelDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading details for channel $channelId")
            // --- TODO: 실제 채널 정보 로드 (repository.getChannelDetails) ---
            delay(500)
            val success = true
            val currentName = "기존 채널 이름 $channelId" // 임시
            val currentType = if (channelId.hashCode() % 2 == 0) ChannelMode.TEXT else ChannelMode.VOICE // Changed to String constants
            // val result = repository.getChannelDetails(channelId)
            // -------------------------------------------------------------
            if (success /*result.isSuccess*/) {
                // val (loadedName, loadedType) = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentChannelName = currentName, // loadedName
                        originalChannelName = currentName, // loadedName
                        currentChannelMode = currentType, // loadedType
                        originalChannelMode = currentType // loadedType
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "채널 정보를 불러오지 못했습니다." // result.exceptionOrNull()?.message
                    )
                }
                // 실패 시 뒤로가기 (선택적)
                // _eventFlow.emit(EditChannelEvent.ShowSnackbar("채널 정보를 불러오지 못했습니다."))
                // _eventFlow.emit(EditChannelEvent.NavigateBack)
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
    fun onChannelTypeSelected(newType: ChannelMode) {
        _uiState.update { it.copy(currentChannelMode = newType) }
    }

    /**
     * 수정 완료 버튼 클릭 시 호출
     */
    fun updateChannel() {
        val currentState = _uiState.value
        val newName = currentState.currentChannelName.trim()
        val newType = currentState.currentChannelMode

        // 이름 유효성 검사
        if (newName.isBlank()) {
            _uiState.update { it.copy(error = "채널 이름을 입력해주세요.") }
            return
        }
        // 변경 여부 확인
        if (newName == currentState.originalChannelName && newType == currentState.originalChannelMode) {
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
            println("ViewModel: Updating channel $channelId to '$newName' (${newType})")

            // --- TODO: 실제 채널 업데이트 로직 (repository.updateChannel) ---
            delay(1000)
            val success = true
            // val result = repository.updateChannel(channelId, newName, newType)
            // --------------------------------------------------------------

            if (success /*result.isSuccess*/) {
                _eventFlow.emit(EditChannelEvent.ShowSnackbar("채널 정보가 수정되었습니다."))
                _uiState.update { it.copy(isLoading = false, updateSuccess = true) }
            } else {
                val errorMessage = "채널 수정 실패" // result.exceptionOrNull()?.message
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

            // --- TODO: 실제 채널 삭제 로직 (repository.deleteChannel) ---
            delay(1000)
            val success = true
            // val result = repository.deleteChannel(channelId)
            // ---------------------------------------------------------

            if (success /*result.isSuccess*/) {
                _eventFlow.emit(EditChannelEvent.ShowSnackbar("채널이 삭제되었습니다."))
                _uiState.update { it.copy(isLoading = false, deleteSuccess = true) }
            } else {
                val errorMessage = "채널 삭제 실패" // result.exceptionOrNull()?.message
                _eventFlow.emit(EditChannelEvent.ShowSnackbar(errorMessage))
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}