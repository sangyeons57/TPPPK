package com.example.feature_settings.viewmodel

// Domain UseCase Import
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.user.UserName
import com.example.domain.usecase.user.UpdateNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UserProfileInfo 정의는 삭제 (Domain 모델 사용 또는 필요 시 UiState에 필드 포함) ---

// --- UI 상태 ---
data class ChangeNameUiState(
    val currentName: String = "", // 현재 이름 (로드 필요 시 추가)
    val newName: UserName = UserName.EMPTY, // 사용자가 입력한 새 이름
    val isLoading: Boolean = false,
    val error: String? = null,
    val updateSuccess: Boolean = false
)

// --- 이벤트 ---
sealed class ChangeNameEvent {
    object DismissDialog : ChangeNameEvent() // 다이얼로그 닫기
    data class ShowSnackbar(val message: String) : ChangeNameEvent()
}

// --- UserProfileRepository 인터페이스 정의는 삭제 (domain/repository/UserRepository.kt 사용) ---

@HiltViewModel
class ChangeNameViewModel @Inject constructor( // ★ 클래스 이름 오타 수정
    private val savedStateHandle: SavedStateHandle,
    private val updateNameUseCase: UpdateNameUseCase // ★ UseCase 주입
) : ViewModel() {

    // 이전 화면이나 SavedStateHandle에서 현재 이름 받아오기 (선택적)
    // private val initialName: String = savedStateHandle["currentName"] ?: ""

    private val _uiState = MutableStateFlow(ChangeNameUiState(/*currentName = initialName*/))
    val uiState: StateFlow<ChangeNameUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ChangeNameEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /** 새 이름 입력 변경 시 호출 */
    fun onNameChange(name: UserName) {
        _uiState.update { it.copy(newName = name, error = null) } // 에러 초기화
    }

    /** 확인 버튼 클릭 시 (이름 업데이트) */
    fun updateUserName() {
        val nameToUpdate = _uiState.value.newName.trim()

        // 유효성 검사 (예: 비어 있는지, 현재 이름과 같은지 등)
        if (nameToUpdate.isBlank()) {
            _uiState.update { it.copy(error = "새 이름을 입력해주세요.") }
            return
        }
        // if (nameToUpdate == _uiState.value.currentName) { // 현재 이름 로드 시 비교 가능
        //     viewModelScope.launch { _eventFlow.emit(ChangeNameEvent.ShowSnackbar("현재 이름과 동일합니다.")) }
        //     return
        // }
        if (_uiState.value.isLoading) return // 중복 요청 방지

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            _eventFlow.emit(ChangeNameEvent.ShowSnackbar("이름 변경 중...")) // 즉각적인 피드백

            val result = updateNameUseCase(nameToUpdate) // ★ UseCase 호출

            when (result) {
                is CustomResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, updateSuccess = true) } // 성공 플래그
                    _eventFlow.emit(ChangeNameEvent.ShowSnackbar("이름이 성공적으로 변경되었습니다."))
                    _eventFlow.emit(ChangeNameEvent.DismissDialog) // 성공 시 다이얼로그 닫기
                }
                is CustomResult.Failure -> {
                    val errorMsg = "이름 변경 실패: ${result.error.message}"
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                    _eventFlow.emit(ChangeNameEvent.ShowSnackbar(errorMsg))
                }
                else -> {
                    val errorMsg = "이름 변경 실패: 알수없는 에러 "
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                    _eventFlow.emit(ChangeNameEvent.ShowSnackbar(errorMsg))
                }
            }
        }
    }
}