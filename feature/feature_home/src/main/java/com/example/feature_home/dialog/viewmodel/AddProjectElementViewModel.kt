package com.example.feature_home.dialog.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.usecase.project.AddCategoryUseCase
import com.example.domain.usecase.project.AddProjectChannelUseCase
import com.example.domain.usecase.project.GetProjectAllCategoriesUseCase
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
    private val getProjectAllCategoriesUseCase: GetProjectAllCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val addProjectChannelUseCase: AddProjectChannelUseCase
) : ViewModel() {

    private var projectId: String? = null

    private val _uiState = MutableStateFlow(AddProjectElementUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddProjectElementEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun initialize(projectId: String) {
        if (this.projectId == null) { // 한 번만 초기화
            this.projectId = projectId
            loadCategoriesForDropdown()
        }
    }

    /**
     * Fetches the list of categories for the current project to populate the selection dropdown.
     */
    private fun loadCategoriesForDropdown() {
        val currentProjectId = projectId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getProjectAllCategoriesUseCase(currentProjectId).first()) {
                is CustomResult.Success -> {
                    val categories = result.data.map { it.category }.sortedBy { it.order }
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
    fun onAddCategory(categoryName: String) {
        val currentProjectId = projectId ?: return
        viewModelScope.launch {
            if (categoryName.isBlank()) {
                _eventFlow.emit(AddProjectElementEvent.ShowSnackbar("Category name cannot be empty."))
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            val result = addCategoryUseCase(
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
    fun onAddChannel(channelName: String, channelType: ProjectChannelType, selectedCategory: Category?) {
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

            val result = addProjectChannelUseCase(
                projectId = currentProjectId,
                channelName = channelName,
                channelType = channelType,
                categoryId = selectedCategory.id
            )

            when (result) {
                is CustomResult.Success -> {
                    _eventFlow.emit(AddProjectElementEvent.ShowSnackbar("Channel '${result.data.channelName}' created successfully."))
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
