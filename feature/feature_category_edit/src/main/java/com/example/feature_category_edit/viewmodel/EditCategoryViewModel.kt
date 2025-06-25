package com.example.feature_category_edit.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core_common.result.CustomResult
import com.example.core_navigation.destination.AppRoutes
import com.example.domain.model.base.Category
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.category.CategoryName
import com.example.domain.model.vo.category.CategoryOrder
import com.example.domain.provider.project.ProjectStructureUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Edit Category screen.
 *
 * @param isLoading True if data is currently being loaded or saved.
 * @param initialCategoryName The original name of the category being edited.
 * @param initialCategoryOrder The original order of the category being edited.
 * @param categoryNameInput Current value in the category name input field.
 * @param categoryOrderInput Current value in the category order input field (as String).
 * @param nameError Error message for the category name input, if any.
 * @param orderError Error message for the category order input, if any.
 * @param generalError General error message not specific to a field.
 * @param allCategoriesInProject List of all categories in the current project, used for validation by UpdateCategoryUseCase.
 * @param isSaveAttempted True if a save operation has been attempted.
 */
data class EditCategoryUiState(
    val isLoading: Boolean = false,
    val initialCategoryName: String = "",
    val initialCategoryOrder: Double = 0.0,
    val categoryNameInput: String = "",
    val categoryOrderInput: String = "",
    val nameError: String? = null,
    val orderError: String? = null,
    val generalError: String? = null,
    val allCategoriesInProject: List<Category> = emptyList(),
    val isSaveAttempted: Boolean = false // Used to trigger one-time events like navigation or snackbar
)

/**
 * Sealed class for events sent from the ViewModel to the UI.
 */
sealed class EditCategoryEvent {
    /**
     * Event indicating that saving the category was successful.
     */
    object SaveSuccess : EditCategoryEvent()

    /**
     * Event to show a snackbar message.
     * @param message The message to display.
     */
    data class ShowSnackbar(val message: String) : EditCategoryEvent()
}

/**
 * ViewModel for the Category Editing screen.
 *
 * Handles fetching category details, validating inputs, and saving changes.
 * @property savedStateHandle Handle to access navigation arguments.
 * @property getCategoryDetailsUseCase Use case to fetch details of a specific category.
 * @property getProjectAllCategoriesUseCase Use case to fetch all categories in a project (for validation context).
 * @property updateCategoryUseCase Use case to update a category.
 */
@HiltViewModel
class EditCategoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val projectStructureUseCaseProvider: ProjectStructureUseCaseProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditCategoryUiState())
    val uiState: StateFlow<EditCategoryUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditCategoryEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val projectId: String? = savedStateHandle[AppRoutes.Project.ARG_PROJECT_ID]
    private val categoryId: String? = savedStateHandle[AppRoutes.Project.ARG_CATEGORY_ID]
    private var originalCategory: Category? = null

    // Provider를 통해 생성된 UseCase 그룹
    private val projectStructureUseCases = projectId?.let {
        projectStructureUseCaseProvider.createForProject(it)
    }

    init {
        if (projectId != null && categoryId != null && projectStructureUseCases != null) {
            loadCategoryDetails(projectId, categoryId)
            loadAllCategoriesInProject(projectId)
        } else {
            _uiState.update { it.copy(generalError = "Project or Category ID is missing.", isLoading = false) }
        }
    }

    /**
     * Loads the details of the category to be edited.
     * @param projId The ID of the project.
     * @param catId The ID of the category.
     */
    private fun loadCategoryDetails(projId: String, catId: String) {
        val useCases = projectStructureUseCases ?: return
        viewModelScope.launch {
            val result = useCases.getCategoryDetailsUseCase(projId, catId);
            _uiState.update { it.copy(isLoading = true) }
            when (result) {
                is CustomResult.Success -> {
                    originalCategory = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            initialCategoryName = result.data.name.value,
                            initialCategoryOrder = result.data.order.value,
                            categoryNameInput = result.data.name.value,
                            categoryOrderInput = result.data.order.value.toString(),
                            generalError = null
                        )
                    }
                }
                is CustomResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, generalError = result.error.message ?: "Failed to load category details.") }
                }
                else -> {
                    Log.e("EditCategoryViewModel", "Unknown result type")
                }
            }
        }
    }

    /**
     * Loads all categories in the current project to be used for validation context.
     * @param projId The ID of the project.
     */
    private fun loadAllCategoriesInProject(projId: String) {
        val useCases = projectStructureUseCases ?: return
        viewModelScope.launch {
            useCases.getProjectAllCategoriesUseCase(projId).map { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val categories = result.data
                        _uiState.update { it.copy(allCategoriesInProject = categories.map { it.category } ) }
                    }
                    is CustomResult.Failure -> {
                        _uiState.update { it.copy(generalError = (_uiState.value.generalError ?: "") + "\nFailed to load project categories for validation: ${result.error.message}") }
                    }
                    is CustomResult.Loading, is CustomResult.Initial, is CustomResult.Progress -> Unit
                }
            }
        }
    }

    /**
     * Called when the category name input changes.
     * @param newName The new name entered by the user.
     */
    fun onNameChange(newName: String) {
        _uiState.update { it.copy(categoryNameInput = newName, nameError = null, isSaveAttempted = false) }
    }

    /**
     * Called when the category order input changes.
     * @param newOrder The new order entered by the user (as a String).
     */
    fun onOrderChange(newOrder: String) {
        _uiState.update { it.copy(categoryOrderInput = newOrder, orderError = null, isSaveAttempted = false) }
    }

    /**
     * Attempts to save the category changes.
     * Performs validation before calling the update use case.
     */
    fun saveCategory() {
        val currentProjectId = projectId ?: return
        val currentOriginalCategory = originalCategory ?: return

        val nameInput = _uiState.value.categoryNameInput
        val orderInputString = _uiState.value.categoryOrderInput

        val orderInputDouble = orderInputString.toDoubleOrNull()
        if (orderInputDouble == null) {
            _uiState.update { it.copy(orderError = "Order must be a valid number.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, isSaveAttempted = true, generalError = null, nameError = null, orderError = null) }

        val useCases = projectStructureUseCases ?: return
        
        viewModelScope.launch {
            when (val result = useCases.updateCategoryUseCase(
                projectId = DocumentId(currentProjectId),
                categoryToUpdate = currentOriginalCategory,
                newName = CategoryName(nameInput),
                newOrder = CategoryOrder(orderInputDouble),
                totalCategories = _uiState.value.allCategoriesInProject.count(),
            )) {
                is CustomResult.Success -> {
                    _eventFlow.emit(EditCategoryEvent.SaveSuccess)
                }
                is CustomResult.Failure -> {
                    val errorMessage = result.error.message ?: "Failed to update category."
                    if (errorMessage.contains("name", ignoreCase = true)) {
                        _uiState.update { it.copy(nameError = errorMessage) }
                    } else if (errorMessage.contains("order", ignoreCase = true)) {
                        _uiState.update { it.copy(orderError = errorMessage) }
                    } else {
                        _uiState.update { it.copy(generalError = errorMessage) }
                    }
                    _eventFlow.emit(EditCategoryEvent.ShowSnackbar(errorMessage))
                }
                is CustomResult.Loading, is CustomResult.Initial, is CustomResult.Progress -> Unit
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
