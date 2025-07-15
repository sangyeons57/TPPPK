package com.example.feature_home.dialog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder
import com.example.domain.provider.project.ProjectChannelUseCaseProvider
import com.example.domain.provider.project.ProjectStructureUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AddProjectElementDialogViewModel: 프로젝트 요소(카테고리/채널) 생성 다이얼로그의 ViewModel
 */
@HiltViewModel
class AddProjectElementDialogViewModel @Inject constructor(
    private val projectStructureUseCaseProvider: ProjectStructureUseCaseProvider,
    private val projectChannelUseCaseProvider: ProjectChannelUseCaseProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProjectElementDialogUiState())
    val uiState: StateFlow<AddProjectElementDialogUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddProjectElementDialogEvent>()
    val eventFlow: SharedFlow<AddProjectElementDialogEvent> = _eventFlow.asSharedFlow()

    private var projectId: DocumentId? = null

    /**
     * 다이얼로그 초기화
     * 
     * @param projectId 프로젝트 ID
     */
    fun initialize(projectId: String) {
        if (projectId.isBlank()) {
            return
        }
        
        this.projectId = DocumentId(projectId)
        
        // 프로젝트 구조 로드하여 카테고리 목록 가져오기
        loadProjectCategories()
    }

    /**
     * 탭 변경 처리
     * 
     * @param selectedTab 선택된 탭 (0: 카테고리, 1: 채널)
     */
    fun onTabChanged(selectedTab: Int) {
        val tabType = when (selectedTab) {
            0 -> CreateElementType.CATEGORY
            1 -> CreateElementType.CHANNEL
            else -> CreateElementType.CATEGORY
        }
        
        _uiState.value = _uiState.value.copy(
            selectedTab = tabType,
            // 탭 변경 시 입력값 초기화
            categoryName = "",
            channelName = "",
            selectedCategoryId = null,
            selectedChannelType = ProjectChannelType.MESSAGES,
            categoryNameError = null,
            channelNameError = null
        )
    }

    /**
     * 카테고리 이름 변경 처리
     */
    fun onCategoryNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(
            categoryName = name,
            categoryNameError = null
        )
    }

    /**
     * 채널 이름 변경 처리
     */
    fun onChannelNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(
            channelName = name,
            channelNameError = null
        )
    }

    /**
     * 채널 카테고리 변경 처리
     */
    fun onChannelCategoryChanged(categoryId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = categoryId
        )
    }

    /**
     * 채널 타입 변경 처리
     */
    fun onChannelTypeChanged(channelType: ProjectChannelType) {
        _uiState.value = _uiState.value.copy(
            selectedChannelType = channelType
        )
    }

    /**
     * 카테고리 생성 처리
     */
    fun onCreateCategory() {
        val currentState = _uiState.value
        val projectId = this.projectId ?: return

        // 카테고리 이름 검증
        if (currentState.categoryName.isBlank()) {
            _uiState.value = currentState.copy(
                categoryNameError = "카테고리 이름을 입력해주세요."
            )
            return
        }

        if (currentState.categoryName.length > 50) {
            _uiState.value = currentState.copy(
                categoryNameError = "카테고리 이름은 50자 이하로 입력해주세요."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true)

            try {
                val structureUseCases = projectStructureUseCaseProvider.createForProject(projectId)
                val categoryName = CategoryName(currentState.categoryName.trim())
                
                when (val result = structureUseCases.addCategoryUseCase(projectId, categoryName)) {
                    is CustomResult.Success -> {
                        _uiState.value = currentState.copy(isLoading = false)
                        _eventFlow.emit(AddProjectElementDialogEvent.CategoryCreated(result.data))
                        _eventFlow.emit(AddProjectElementDialogEvent.DismissDialog)
                    }
                    is CustomResult.Failure -> {
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            categoryNameError = result.error.message ?: "카테고리 생성에 실패했습니다."
                        )
                    }
                    else -> {
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            categoryNameError = "카테고리 생성에 실패했습니다."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    categoryNameError = "카테고리 생성 중 오류가 발생했습니다."
                )
            }
        }
    }

    /**
     * 채널 생성 처리
     */
    fun onCreateChannel() {
        val currentState = _uiState.value
        val projectId = this.projectId ?: return

        // 채널 이름 검증
        if (currentState.channelName.isBlank()) {
            _uiState.value = currentState.copy(
                channelNameError = "채널 이름을 입력해주세요."
            )
            return
        }

        if (currentState.channelName.length > 50) {
            _uiState.value = currentState.copy(
                channelNameError = "채널 이름은 50자 이하로 입력해주세요."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true)

            try {
                val channelUseCases = projectChannelUseCaseProvider.createForProject(projectId)
                val channelName = Name(currentState.channelName.trim())
                
                // 카테고리 ID 처리 (null이면 NO_CATEGORY_ID 사용)
                val categoryId = currentState.selectedCategoryId 
                    ?: com.example.domain.model.base.Category.NO_CATEGORY_ID
                
                when (val result = channelUseCases.addProjectChannelUseCase(
                    projectId = projectId,
                    channelName = channelName,
                    categoryId = DocumentId(categoryId),
                    channelType = currentState.selectedChannelType
                )) {
                    is CustomResult.Success -> {
                        _uiState.value = currentState.copy(isLoading = false)
                        _eventFlow.emit(AddProjectElementDialogEvent.ChannelCreated(result.data))
                        _eventFlow.emit(AddProjectElementDialogEvent.DismissDialog)
                    }
                    is CustomResult.Failure -> {
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            channelNameError = result.error.message ?: "채널 생성에 실패했습니다."
                        )
                    }
                    else -> {
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            channelNameError = "채널 생성에 실패했습니다."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    channelNameError = "채널 생성 중 오류가 발생했습니다."
                )
            }
        }
    }

    /**
     * 다이얼로그 닫기 처리
     */
    fun onDismiss() {
        viewModelScope.launch {
            _eventFlow.emit(AddProjectElementDialogEvent.DismissDialog)
        }
    }

    /**
     * 프로젝트 카테고리 목록 로드
     */
    private fun loadProjectCategories() {
        val projectId = this.projectId ?: return
        
        viewModelScope.launch {
            try {
                val structureUseCases = projectStructureUseCaseProvider.createForProject(projectId)
                when (val result = structureUseCases.getProjectAllCategoriesUseCase().first()) {
                    is CustomResult.Success -> {
                        // UI 레이어에서 NoCategory 추가 처리
                        val categories = result.data.toMutableList()
                        
                        // NoCategory가 없으면 UI 표시용으로 추가
                        val hasNoCategory = categories.any { it.id.value == Category.NO_CATEGORY_ID }
                        if (!hasNoCategory) {
                            val noCategory = Category.fromDataSource(
                                id = com.example.domain.model.vo.DocumentId(Category.NO_CATEGORY_ID),
                                name = com.example.domain.model.vo.category.CategoryName.NO_CATEGORY_NAME,
                                order = com.example.domain.model.vo.category.CategoryOrder(Category.NO_CATEGORY_ORDER),
                                createdBy = com.example.domain.model.vo.OwnerId("system"),
                                createdAt = java.time.Instant.now(),
                                updatedAt = java.time.Instant.now(),
                                isCategory = com.example.domain.model.vo.category.IsCategoryFlag.FALSE
                            )
                            categories.add(0, noCategory)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            availableCategories = categories.sortedBy { it.order.value }
                        )
                    }
                    is CustomResult.Failure -> {
                        // 카테고리 로드 실패 시 NoCategory만 표시
                        val noCategory = Category.fromDataSource(
                            id = com.example.domain.model.vo.DocumentId(Category.NO_CATEGORY_ID),
                            name = com.example.domain.model.vo.category.CategoryName.NO_CATEGORY_NAME,
                            order = com.example.domain.model.vo.category.CategoryOrder(Category.NO_CATEGORY_ORDER),
                            createdBy = com.example.domain.model.vo.OwnerId("system"),
                            createdAt = java.time.Instant.now(),
                            updatedAt = java.time.Instant.now(),
                            isCategory = com.example.domain.model.vo.category.IsCategoryFlag.FALSE
                        )
                        _uiState.value = _uiState.value.copy(
                            availableCategories = listOf(noCategory)
                        )
                    }
                    else -> {
                        // 다른 상태의 경우 NoCategory만 표시
                        val noCategory = Category.fromDataSource(
                            id = com.example.domain.model.vo.DocumentId(Category.NO_CATEGORY_ID),
                            name = com.example.domain.model.vo.category.CategoryName.NO_CATEGORY_NAME,
                            order = com.example.domain.model.vo.category.CategoryOrder(Category.NO_CATEGORY_ORDER),
                            createdBy = com.example.domain.model.vo.OwnerId("system"),
                            createdAt = java.time.Instant.now(),
                            updatedAt = java.time.Instant.now(),
                            isCategory = com.example.domain.model.vo.category.IsCategoryFlag.FALSE
                        )
                        _uiState.value = _uiState.value.copy(
                            availableCategories = listOf(noCategory)
                        )
                    }
                }
            } catch (e: Exception) {
                // 예외 발생 시 NoCategory만 표시
                val noCategory = Category.fromDataSource(
                    id = com.example.domain.model.vo.DocumentId(Category.NO_CATEGORY_ID),
                    name = com.example.domain.model.vo.category.CategoryName.NO_CATEGORY_NAME,
                    order = com.example.domain.model.vo.category.CategoryOrder(Category.NO_CATEGORY_ORDER),
                    createdBy = com.example.domain.model.vo.OwnerId("system"),
                    createdAt = java.time.Instant.now(),
                    updatedAt = java.time.Instant.now(),
                    isCategory = com.example.domain.model.vo.category.IsCategoryFlag.FALSE
                )
                _uiState.value = _uiState.value.copy(
                    availableCategories = listOf(noCategory)
                )
            }
        }
    }
}