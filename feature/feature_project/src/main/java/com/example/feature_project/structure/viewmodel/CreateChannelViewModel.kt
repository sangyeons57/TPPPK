package com.example.teamnovapersonalprojectprojectingkotlin.feature_project_structure.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// 채널 유형 정의
enum class ChannelType { TEXT, VOICE }

// --- UI 상태 ---
data class CreateChannelUiState(
    val channelName: String = "",
    val selectedChannelType: ChannelType = ChannelType.TEXT, // 기본값 텍스트
    val isLoading: Boolean = false,
    val error: String? = null,
    val createSuccess: Boolean = false // 생성 성공 시 네비게이션 트리거
)

// --- 이벤트 ---
sealed class CreateChannelEvent {
    object NavigateBack : CreateChannelEvent()
    data class ShowSnackbar(val message: String) : CreateChannelEvent()
    object ClearFocus : CreateChannelEvent()
}

// --- Repository 인터페이스 (가상 - 이전 ViewModel과 공유) ---
/**
interface ProjectStructureRepository {
    suspend fun createCategory(projectId: String, categoryName: String): Result<Unit>
    suspend fun createChannel(projectId: String, categoryId: String, channelName: String, channelType: ChannelType): Result<Unit>
    // ... (수정/삭제 등) ...
}
**/

@HiltViewModel
class CreateChannelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    // TODO: private val repository: ProjectStructureRepository
) : ViewModel() {

    // 네비게이션으로 전달받은 ID (실제 앱에서는 네비게이션 인자 이름 확인 필요)
    private val projectId: String = savedStateHandle["projectId"] ?: error("projectId가 전달되지 않았습니다.")
    private val categoryId: String = savedStateHandle["categoryId"] ?: error("categoryId가 전달되지 않았습니다.")

    private val _uiState = MutableStateFlow(CreateChannelUiState())
    val uiState: StateFlow<CreateChannelUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CreateChannelEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * 채널 이름 입력 변경 시 호출
     */
    fun onChannelNameChange(name: String) {
        _uiState.update {
            it.copy(channelName = name, error = null) // 에러 초기화
        }
    }

    /**
     * 채널 유형 선택 시 호출
     */
    fun onChannelTypeSelected(type: ChannelType) {
        _uiState.update { it.copy(selectedChannelType = type) }
    }

    /**
     * 완료 버튼 클릭 시 호출
     */
    fun createChannel() {
        val currentName = _uiState.value.channelName.trim()
        val currentType = _uiState.value.selectedChannelType

        // 유효성 검사
        if (currentName.isBlank()) {
            _uiState.update { it.copy(error = "채널 이름을 입력해주세요.") }
            return
        }
        // TODO: 채널 이름 규칙 검사 (예: 특수문자, 공백 등)

        if (_uiState.value.isLoading) return // 로딩 중 중복 실행 방지

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            _eventFlow.emit(CreateChannelEvent.ClearFocus)
            println("ViewModel: Creating ${currentType} channel '$currentName' in project $projectId / category $categoryId")

            // --- TODO: 실제 채널 생성 로직 (repository.createChannel) ---
            delay(1000)
            val success = true
            // val result = repository.createChannel(projectId, categoryId, currentName, currentType)
            // -----------------------------------------------------------

            if (success /*result.isSuccess*/) {
                _eventFlow.emit(CreateChannelEvent.ShowSnackbar("채널이 생성되었습니다."))
                _uiState.update { it.copy(isLoading = false, createSuccess = true) }
            } else {
                val errorMessage = "채널 생성 실패" // result.exceptionOrNull()?.message ?: "채널 생성 실패"
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }
}