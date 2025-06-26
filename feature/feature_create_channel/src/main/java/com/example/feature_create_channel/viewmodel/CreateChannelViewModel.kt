package com.example.feature_create_channel.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.domain.provider.project.ProjectChannelUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 채널 유형 정의 -> domain/model/ChannelType 으로 이동했으므로 제거
// enum class ChannelType { TEXT, VOICE }

// --- UI 상태 ---
data class CreateChannelUiState(
    val channelName: Name = Name.EMPTY,
    val selectedChannelMode: ProjectChannelType = ProjectChannelType.MESSAGES, // Changed to String, default TEXT mode
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


@HiltViewModel
class CreateChannelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectChannelUseCaseProvider: ProjectChannelUseCaseProvider
) : ViewModel() {

    // SavedStateHandle 확장 함수와 AppDestination 상수를 사용하여 ID들 가져오기
    private val projectId: String = savedStateHandle.getRequiredString(RouteArgs.PROJECT_ID)
    private val categoryId: String = savedStateHandle.getRequiredString(RouteArgs.CATEGORY_ID)

    // Provider를 통해 생성된 UseCase 그룹
    private val projectChannelUseCases =
        projectChannelUseCaseProvider.createForProject(DocumentId(projectId))

    private val _uiState = MutableStateFlow(CreateChannelUiState())
    val uiState: StateFlow<CreateChannelUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CreateChannelEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * 채널 이름 입력 변경 시 호출
     */
    fun onChannelNameChange(name: String) {
        _uiState.update {
            it.copy(channelName = Name.from(name), error = null) // 에러 초기화
        }
    }

    /**
     * 채널 유형 선택 시 호출
     */
    fun onChannelTypeSelected(type: ProjectChannelType) { // Changed ChannelType to String
        _uiState.update { it.copy(selectedChannelMode = type) }
    }

    /**
     * 완료 버튼 클릭 시 호출
     */
    fun createChannel() {
        val currentName = _uiState.value.channelName.trim()
        val currentType = _uiState.value.selectedChannelMode

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

            when (val result = projectChannelUseCases.createProjectChannelUseCase(
                name = currentName,
                order = ProjectChannelOrder.DEFAULT,
                channelType = currentType

            )) {
                is CustomResult.Success -> {
                    _eventFlow.emit(CreateChannelEvent.ShowSnackbar("채널이 생성되었습니다: $currentName"))
                    _uiState.update { it.copy(isLoading = false, createSuccess = true) }
                }
                is CustomResult.Failure -> {
                    val errorMessage = result.error.message ?: "채널 생성 실패"
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false, error = "채널 생성 실패") }
                }
            }
        }
    }
}