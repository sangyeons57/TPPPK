package com.example.feature_home.dialog.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.provider.project.ProjectChannelUseCaseProvider
import com.example.domain.provider.project.ProjectStructureUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the AddProjectElementDialog.
 * @property isLoading Indicates if an operation is in progress.
 * @property availableCategories The list of categories for the dropdown.
 * @property errorMessage An error message to be displayed, if any.
 */
data class AddProjectElementUiState(
    val isLoading: Boolean = false,
    val availableCategories: List<Category> = emptyList(),
    val errorMessage: String? = null
)

/**
 * One-time events from the ViewModel to the UI.
 */
sealed class AddProjectElementEvent {
    /**
     * Signals the UI to show a snackbar message.
     * @param message The message to display.
     */
    data class ShowSnackbar(val message: String) : AddProjectElementEvent()

    /**
     * Signals the UI to dismiss the dialog.
     */
    object DismissDialog : AddProjectElementEvent()
}

/**
 * ViewModel for the AddProjectElementDialog.
 * Handles the business logic for adding new categories and channels to a project.
 */
@HiltViewModel
class AddProjectElementViewModel @Inject constructor(
    private val projectStructureUseCaseProvider: ProjectStructureUseCaseProvider,
    private val projectChannelUseCaseProvider: ProjectChannelUseCaseProvider
) : ViewModel() {

    private var projectId: DocumentId? = null
    private var projectStructureUseCases: com.example.domain.provider.project.ProjectStructureUseCases? =
        null
    private var projectChannelUseCases: com.example.domain.provider.project.ProjectChannelUseCases? =
        null

    private val _uiState = MutableStateFlow(AddProjectElementUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddProjectElementEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun initialize(projectId: DocumentId) {
        if (this.projectId == null) { // 한 번만 초기화
            this.projectId = projectId
            // Provider를 통해 UseCase 그룹 생성
            this.projectStructureUseCases =
                projectStructureUseCaseProvider.createForCurrentUser(projectId)
            // Note: projectChannelUseCases는 채널 추가 시 categoryId와 함께 생성됨
            loadCategoriesForDropdown()
        }
    }

    /**
     * Fetches the list of categories for the current project to populate the selection dropdown.
     */
    private fun loadCategoriesForDropdown() {
        val currentProjectId = projectId ?: return
        val useCases = projectStructureUseCases ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = useCases.getProjectAllCategoriesUseCase().first()) {
                is CustomResult.Success -> {
                    val categories = result.data.sortedBy { it.order.value }
                    _uiState.update {
                        it.copy(isLoading = false, availableCategories = categories)
                    }
                }
                is CustomResult.Failure -> {
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                    _eventFlow.emit(AddProjectElementEvent.ShowSnackbar("Error: Failed to load categories."))
                }
                else -> {
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                    _eventFlow.emit(AddProjectElementEvent.ShowSnackbar("Error: Unknown error."))
                }
            }
        }
    }

    /**
     * Handles the logic to add a new category.
     * @param categoryName The name for the new category.
     */
    fun onAddCategory(categoryName: CategoryName) {
        val currentProjectId = projectId ?: return
        val useCases = projectStructureUseCases ?: return
        viewModelScope.launch {
            if (categoryName.isBlank()) {
                _eventFlow.emit(AddProjectElementEvent.ShowSnackbar("Category name cannot be empty."))
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            val result = useCases.addCategoryUseCase(
                projectId = currentProjectId,
                categoryName = categoryName
            )
            when (result) {
                is CustomResult.Success -> {
                    _eventFlow.emit(AddProjectElementEvent.ShowSnackbar("Category '${result.data.name}' created successfully."))
                    _eventFlow.emit(AddProjectElementEvent.DismissDialog)
                    _uiState.update { it.copy(isLoading = false) }
                }
                is CustomResult.Failure -> {
                    _eventFlow.emit(AddProjectElementEvent.ShowSnackbar("Error: ${result.error.message}"))
                    _uiState.update { it.copy(isLoading = false) }
                }
                else -> {
                    _eventFlow.emit(AddProjectElementEvent.ShowSnackbar("Error: Unknown error."))
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    /**
     * Handles the logic to add a new channel.
     * @param channelName The name for the new channel.
     * @param channelType The type of the new channel.
     * @param selectedCategory The category to which the channel will be added.
     */
    fun onAddChannel(
        channelName: Name,
        channelType: ProjectChannelType,
        selectedCategory: Category?
    ) {
        val currentProjectId = projectId ?: return
        viewModelScope.launch {
            if (channelName.isBlank()) {
                _eventFlow.emit(AddProjectElementEvent.ShowSnackbar("Channel name cannot be empty."))
                return@launch
            }
            if (selectedCategory == null) {
                _eventFlow.emit(AddProjectElementEvent.ShowSnackbar("A category must be selected."))
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true) }

            // 채널 추가 시 프로젝트 채널 UseCases 생성
            val channelUseCases = projectChannelUseCaseProvider.createForProject(currentProjectId)

            val result = channelUseCases.addProjectChannelUseCase(
                projectId = currentProjectId,
                channelName = channelName,
                channelType = channelType,
                categoryId = selectedCategory.id
            )

            when (result) {
                is CustomResult.Success -> {
                    _eventFlow.emit(AddProjectElementEvent.ShowSnackbar("Channel '${result.data}' created successfully."))
                    _eventFlow.emit(AddProjectElementEvent.DismissDialog)
                    _uiState.update { it.copy(isLoading = false) }
                }
                is CustomResult.Failure -> {
                    _eventFlow.emit(AddProjectElementEvent.ShowSnackbar("Error: ${result.error.message}"))
                    _uiState.update { it.copy(isLoading = false) }
                }
                else  -> {
                    _eventFlow.emit(AddProjectElementEvent.ShowSnackbar("Error: Unknown error."))
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}
