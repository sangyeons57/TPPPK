package com.example.feature_profile.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.UserStatus
import com.example.domain.usecase.user.GetCurrentStatusUseCase
import com.example.domain.usecase.user.UpdateUserStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 상태 ---
data class ChangeStatusUiState(
    val currentStatus: UserStatus? = null, // 현재 상태 (초기 로드 전 null)
    val selectedStatus: UserStatus? = null, // 사용자가 선택한 상태 (null 가능하도록 변경)
    val availableStatuses: List<UserStatus> = UserStatus.entries, // 선택 가능한 상태 목록
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val error: String? = null,
    val updateSuccess: Boolean = false
)

// --- 이벤트 ---
sealed class ChangeStatusEvent {
    object DismissDialog : ChangeStatusEvent()
    data class ShowSnackbar(val message: String) : ChangeStatusEvent()
}

/**
 * 상태 변경 화면을 위한 ViewModel
 */
@HiltViewModel
class ChangeStatusViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getCurrentStatusUseCase: GetCurrentStatusUseCase,
    private val updateUserStatusUseCase: UpdateUserStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangeStatusUiState(isLoading = true))
    val uiState: StateFlow<ChangeStatusUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ChangeStatusEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadCurrentStatus()
    }

    /** 현재 상태 로드 */
    private fun loadCurrentStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading current user status")

            getCurrentStatusUseCase().collect { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val status = result.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentStatus = status,
                                selectedStatus = status
                            )
                        }
                    }
                    is CustomResult.Failure -> {
                        val errorMsg = "현재 상태를 불러오지 못했습니다: ${result.error.message}"
                        _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                        _eventFlow.emit(ChangeStatusEvent.ShowSnackbar(errorMsg))
                    }
                    else -> { /* 로딩 상태 등 다른 상태는 무시 */ }
                }
            }
        }
    }

    /** 사용자가 상태를 선택했을 때 호출 */
    fun onStatusSelected(status: UserStatus) {
        _uiState.update { it.copy(selectedStatus = status) }
    }

    /** 확인 버튼 클릭 시 (상태 업데이트) */
    fun updateStatus() {
        val statusToUpdate = _uiState.value.selectedStatus
        val currentStatus = _uiState.value.currentStatus

        if (statusToUpdate == null) {
            viewModelScope.launch { _eventFlow.emit(ChangeStatusEvent.ShowSnackbar("상태를 선택해주세요.")) }
            return
        }

        if (statusToUpdate == currentStatus) {
            viewModelScope.launch { _eventFlow.emit(ChangeStatusEvent.ShowSnackbar("현재 상태와 동일합니다.")) }
            return
        }

        if (_uiState.value.isUpdating) return // 중복 업데이트 방지

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }
            _eventFlow.emit(ChangeStatusEvent.ShowSnackbar("상태 변경 중..."))

            // UserStatus 객체를 전달하도록 수정
            val result = updateUserStatusUseCase(statusToUpdate)

            when (result) {
                is CustomResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            currentStatus = statusToUpdate, // 성공 시 현재 상태도 업데이트
                            updateSuccess = true
                        )
                    }
                    _eventFlow.emit(ChangeStatusEvent.ShowSnackbar("상태가 '${statusToUpdate.name}'(으)로 변경되었습니다."))
                    _eventFlow.emit(ChangeStatusEvent.DismissDialog) // 성공 시 다이얼로그 닫기
                }
                is CustomResult.Failure -> {
                    val errorMsg = "상태 변경 실패: ${result.error.message}"
                    _uiState.update { it.copy(isUpdating = false, error = errorMsg) }
                    _eventFlow.emit(ChangeStatusEvent.ShowSnackbar(errorMsg))
                }
                else -> {
                    // 기타 상태 처리 (로딩 등)
                }
            }
        }
    }
}