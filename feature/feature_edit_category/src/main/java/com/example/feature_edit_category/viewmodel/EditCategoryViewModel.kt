package com.example.feature_edit_category.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.provider.project.ProjectStructureUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 상태 ---
data class EditCategoryUiState(
    val categoryId: String = "", // 편집 대상 ID
    val currentCategoryName: String = "", // 현재(또는 수정 중인) 이름
    val originalCategoryName: String = "", // 초기 로드된 이름 (변경 여부 확인용)
    val currentCategoryOrder: Double = 0.0, // 현재 카테고리 순서
    val originalCategoryOrder: Double = 0.0, // 초기 로드된 순서 (변경 여부 확인용)
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


@HiltViewModel
class EditCategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectStructureUseCaseProvider: ProjectStructureUseCaseProvider
) : ViewModel() {

    private val projectId: String = savedStateHandle.getRequiredString(RouteArgs.PROJECT_ID)
    private val categoryId: String = savedStateHandle.getRequiredString(RouteArgs.CATEGORY_ID)
    
    // UseCase 초기화
    private val structureUseCases by lazy {
        projectStructureUseCaseProvider.createForProject(DocumentId(projectId))
    }

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
            
            when (val result = structureUseCases.getCategoryDetailsUseCase(projectId, categoryId)) {
                is CustomResult.Success -> {
                    val category = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentCategoryName = category.name.value,
                            originalCategoryName = category.name.value,
                            currentCategoryOrder = category.order.value,
                            originalCategoryOrder = category.order.value
                        )
                    }
                }
                is CustomResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "카테고리 정보를 불러오지 못했습니다: ${result.error.message}"
                        )
                    }
                    _eventFlow.emit(EditCategoryEvent.ShowSnackbar("카테고리 정보를 불러오지 못했습니다."))
                }
                is CustomResult.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "알 수 없는 오류가 발생했습니다."
                        )
                    }
                }
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
     * 카테고리 순서 입력 변경 시 호출
     */
    fun onCategoryOrderChange(newOrder: String) {
        val orderValue = newOrder.toDoubleOrNull()
        if (orderValue != null) {
            _uiState.update { it.copy(currentCategoryOrder = orderValue, error = null) }
        } else {
            _uiState.update { it.copy(error = "올바른 숫자를 입력해주세요.") }
        }
    }

    /**
     * 수정 완료 버튼 클릭 시 호출
     */
    fun updateCategory() {
        val currentName = _uiState.value.currentCategoryName.trim()
        val originalName = _uiState.value.originalCategoryName
        val currentOrder = _uiState.value.currentCategoryOrder
        val originalOrder = _uiState.value.originalCategoryOrder

        // 이름 유효성 검사
        if (currentName.isBlank()) {
            _uiState.update { it.copy(error = "카테고리 이름을 입력해주세요.") }
            return
        }
        
        // 순서 유효성 검사
        if (currentOrder < 0.0) {
            _uiState.update { it.copy(error = "카테고리 순서는 0 이상이어야 합니다.") }
            return
        }
        
        // 변경 여부 확인 (변경 없으면 요청 안 함)
        if (currentName == originalName && currentOrder == originalOrder) {
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

            // 먼저 현재 카테고리 정보를 가져온 후 업데이트
            when (val getCategoryResult = structureUseCases.getCategoryDetailsUseCase(projectId, categoryId)) {
                is CustomResult.Success -> {
                    val categoryToUpdate = getCategoryResult.data
                    val newCategoryName = CategoryName(currentName)
                    val newCategoryOrder = CategoryOrder(currentOrder)
                    
                    when (val updateResult = structureUseCases.updateCategoryUseCase(
                        projectId = DocumentId(projectId),
                        categoryToUpdate = categoryToUpdate,
                        newName = newCategoryName,
                        newOrder = newCategoryOrder,
                        totalCategories = 10 // TODO: 실제 카테고리 수 계산 필요
                    )) {
                        is CustomResult.Success -> {
                            _eventFlow.emit(EditCategoryEvent.ShowSnackbar("카테고리 이름이 수정되었습니다."))
                            _uiState.update { it.copy(isLoading = false, updateSuccess = true) }
                        }
                        is CustomResult.Failure -> {
                            val errorMessage = "카테고리 수정 실패: ${updateResult.error.message}"
                            _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                        }
                        else -> {
                            _uiState.update { it.copy(isLoading = false, error = "카테고리 수정 중 오류가 발생했습니다.") }
                        }
                    }
                }
                is CustomResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = "카테고리 정보를 가져올 수 없습니다: ${getCategoryResult.error.message}") }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false, error = "카테고리 정보를 가져오는 중 오류가 발생했습니다.") }
                }
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

            when (val result = structureUseCases.deleteCategoryUseCase(DocumentId(categoryId))) {
                is CustomResult.Success -> {
                    _eventFlow.emit(EditCategoryEvent.ShowSnackbar("카테고리가 삭제되었습니다."))
                    _uiState.update { it.copy(isLoading = false, deleteSuccess = true) }
                }
                is CustomResult.Failure -> {
                    val errorMessage = "카테고리 삭제 실패: ${result.error.message}"
                    _eventFlow.emit(EditCategoryEvent.ShowSnackbar(errorMessage))
                    _uiState.update { it.copy(isLoading = false) }
                }
                else -> {
                    _eventFlow.emit(EditCategoryEvent.ShowSnackbar("카테고리 삭제 중 오류가 발생했습니다."))
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}