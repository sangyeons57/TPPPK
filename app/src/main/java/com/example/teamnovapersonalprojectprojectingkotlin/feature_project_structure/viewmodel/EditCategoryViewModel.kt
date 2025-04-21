package com.example.teamnovapersonalprojectprojectingkotlin.feature_project_structure.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 상태 ---
data class EditCategoryUiState(
    val categoryId: String = "", // 편집 대상 ID
    val currentCategoryName: String = "", // 현재(또는 수정 중인) 이름
    val originalCategoryName: String = "", // 초기 로드된 이름 (변경 여부 확인용)
    val isLoading: Boolean = false, // 로딩 상태 (초기 로드 또는 업데이트/삭제)
    val error: String? = null, // 오류 메시지
    val updateSuccess: Boolean = false, // 업데이트 성공 시 네비게이션 트리거
    val deleteSuccess: Boolean = false // 삭제 성공 시 네비게이션 트리거
)

// --- 이벤트 ---
sealed class EditCategoryEvent {
    object NavigateBack : EditCategoryEvent()
    data class ShowSnackbar(val message: String) : EditCategoryEvent()
    object ClearFocus : EditCategoryEvent()
    object ShowDeleteConfirmation : EditCategoryEvent() // 삭제 확인 다이얼로그 표시 요청
}

// --- Repository 인터페이스 (가상 - 이전 ViewModel과 공유 또는 확장) ---
/**
interface ProjectStructureRepository {
    suspend fun createCategory(projectId: String, categoryName: String): Result<Unit>
    suspend fun createChannel(projectId: String, categoryId: String, channelName: String, channelType: ChannelType): Result<Unit>
    suspend fun getCategoryDetails(categoryId: String): Result<String> // 카테고리 이름 반환 가정
    suspend fun updateCategory(categoryId: String, newName: String): Result<Unit>
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    // ... (채널 수정/삭제 등) ...
}
**/

@HiltViewModel
class EditCategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    // TODO: private val repository: ProjectStructureRepository
) : ViewModel() {

    // 네비게이션으로 전달받은 ID (실제 앱에서는 네비게이션 인자 이름 확인 필요)
    private val projectId: String = savedStateHandle["projectId"] ?: error("projectId가 전달되지 않았습니다.")
    private val categoryId: String = savedStateHandle["categoryId"] ?: error("categoryId가 전달되지 않았습니다.")

    private val _uiState = MutableStateFlow(EditCategoryUiState(categoryId = categoryId, isLoading = true)) // 초기 로딩 상태
    val uiState: StateFlow<EditCategoryUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditCategoryEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadCategoryDetails()
    }

    /**
     * 초기 카테고리 정보 로드
     */
    private fun loadCategoryDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading details for category $categoryId")
            // --- TODO: 실제 카테고리 정보 로드 (repository.getCategoryDetails) ---
            delay(500) // 임시 딜레이
            val success = true
            val currentName = "기존 카테고리 이름 $categoryId" // 임시 데이터
            // val result = repository.getCategoryDetails(categoryId)
            // -------------------------------------------------------------------
            if (success /*result.isSuccess*/) {
                // val loadedName = result.getOrThrow() // 실제 데이터 사용
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentCategoryName = currentName, // loadedName,
                        originalCategoryName = currentName // loadedName
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "카테고리 정보를 불러오지 못했습니다." // result.exceptionOrNull()?.message
                    )
                }
                // 로드 실패 시 이전 화면으로 돌아가도록 이벤트 발생 (선택적)
                // _eventFlow.emit(EditCategoryEvent.ShowSnackbar("카테고리 정보를 불러오지 못했습니다."))
                // _eventFlow.emit(EditCategoryEvent.NavigateBack)
            }
        }
    }

    /**
     * 카테고리 이름 입력 변경 시 호출
     */
    fun onCategoryNameChange(newName: String) {
        _uiState.update {
            it.copy(currentCategoryName = newName, error = null) // 에러 초기화
        }
    }

    /**
     * 수정 완료 버튼 클릭 시 호출
     */
    fun updateCategory() {
        val currentName = _uiState.value.currentCategoryName.trim()
        val originalName = _uiState.value.originalCategoryName

        // 이름 유효성 검사
        if (currentName.isBlank()) {
            _uiState.update { it.copy(error = "카테고리 이름을 입력해주세요.") }
            return
        }
        // 이름 변경 여부 확인 (변경 없으면 요청 안 함)
        if (currentName == originalName) {
            viewModelScope.launch {
                _eventFlow.emit(EditCategoryEvent.ShowSnackbar("변경된 내용이 없습니다."))
                _eventFlow.emit(EditCategoryEvent.NavigateBack) // 변경 없으면 그냥 뒤로 가기
            }
            return
        }

        if (_uiState.value.isLoading) return // 로딩 중 중복 실행 방지

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            _eventFlow.emit(EditCategoryEvent.ClearFocus)
            println("ViewModel: Updating category $categoryId to '$currentName'")

            // --- TODO: 실제 카테고리 업데이트 로직 (repository.updateCategory) ---
            delay(1000)
            val success = true
            // val result = repository.updateCategory(categoryId, currentName)
            // ----------------------------------------------------------------

            if (success /*result.isSuccess*/) {
                _eventFlow.emit(EditCategoryEvent.ShowSnackbar("카테고리 이름이 수정되었습니다."))
                _uiState.update { it.copy(isLoading = false, updateSuccess = true) } // 성공 및 네비게이션 트리거
            } else {
                val errorMessage = "카테고리 수정 실패" // result.exceptionOrNull()?.message
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    /**
     * 삭제 버튼 클릭 시 호출 (TopAppBar Action)
     */
    fun onDeleteClick() {
        viewModelScope.launch {
            _eventFlow.emit(EditCategoryEvent.ShowDeleteConfirmation)
        }
    }

    /**
     * 삭제 확인 다이얼로그에서 '삭제' 버튼 클릭 시 호출
     */
    fun confirmDelete() {
        if (_uiState.value.isLoading) return // 로딩 중 중복 실행 방지

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Deleting category $categoryId")

            // --- TODO: 실제 카테고리 삭제 로직 (repository.deleteCategory) ---
            delay(1000)
            val success = true
            // val result = repository.deleteCategory(categoryId)
            // ------------------------------------------------------------

            if (success /*result.isSuccess*/) {
                _eventFlow.emit(EditCategoryEvent.ShowSnackbar("카테고리가 삭제되었습니다."))
                _uiState.update { it.copy(isLoading = false, deleteSuccess = true) } // 성공 및 네비게이션 트리거
            } else {
                val errorMessage = "카테고리 삭제 실패" // result.exceptionOrNull()?.message
                _eventFlow.emit(EditCategoryEvent.ShowSnackbar(errorMessage)) // 스낵바로 에러 알림
                _uiState.update { it.copy(isLoading = false) } // 로딩 상태만 해제
            }
        }
    }
}