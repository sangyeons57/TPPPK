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

// --- UI 상태 ---
data class CreateCategoryUiState(
    val categoryName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val createSuccess: Boolean = false // 생성 성공 시 네비게이션 트리거
)

// --- 이벤트 ---
sealed class CreateCategoryEvent {
    object NavigateBack : CreateCategoryEvent()
    data class ShowSnackbar(val message: String) : CreateCategoryEvent()
    object ClearFocus : CreateCategoryEvent()
}

// --- Repository 인터페이스 (가상) ---
/**
interface ProjectStructureRepository { // 프로젝트 구조 관련 Repository
    suspend fun createCategory(projectId: String, categoryName: String): Result<Unit> // Result 래퍼 사용 권장
    // ... (채널 생성/수정/삭제, 카테고리 수정/삭제 등) ...
}
**/

@HiltViewModel
class CreateCategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    // TODO: private val repository: ProjectStructureRepository
) : ViewModel() {

    // SavedStateHandle 확장 함수와 AppDestination 상수를 사용하여 projectId 가져오기
    private val projectId: String = savedStateHandle.getRequiredString(AppRoutes.Project.ARG_PROJECT_ID)

    private val _uiState = MutableStateFlow(CreateCategoryUiState())
    val uiState: StateFlow<CreateCategoryUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CreateCategoryEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * 카테고리 이름 입력 변경 시 호출
     */
    fun onCategoryNameChange(name: String) {
        _uiState.update {
            // 이름 변경 시 에러 메시지 초기화
            it.copy(categoryName = name, error = null)
        }
    }

    /**
     * 완료 버튼 클릭 시 호출
     */
    fun createCategory() {
        val currentName = _uiState.value.categoryName.trim()

        // 유효성 검사
        if (currentName.isBlank()) {
            _uiState.update { it.copy(error = "카테고리 이름을 입력해주세요.") }
            return
        }

        // 이미 로딩 중이면 중복 실행 방지
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            _eventFlow.emit(CreateCategoryEvent.ClearFocus) // 키보드 숨기기 요청
            println("ViewModel: Creating category '$currentName' for project $projectId")

            // --- TODO: 실제 카테고리 생성 로직 (repository.createCategory) ---
            delay(1000) // 임시 딜레이
            val success = true // Random.nextBoolean()
            // val result = repository.createCategory(projectId, currentName)
            // -----------------------------------------------------------------

            if (success /*result.isSuccess*/) {
                _eventFlow.emit(CreateCategoryEvent.ShowSnackbar("카테고리가 생성되었습니다."))
                _uiState.update { it.copy(isLoading = false, createSuccess = true) } // 성공 상태 업데이트 -> 네비게이션 트리거
            } else {
                val errorMessage = "카테고리 생성 실패" // result.exceptionOrNull()?.message ?: "카테고리 생성 실패"
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }
}