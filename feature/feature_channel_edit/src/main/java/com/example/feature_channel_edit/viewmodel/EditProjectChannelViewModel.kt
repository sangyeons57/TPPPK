package com.example.feature_channel_edit.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.destination.AppRoutes
import com.example.domain.model.base.ProjectChannel
import com.example.domain.provider.project.ProjectChannelUseCaseProvider
import com.example.domain.provider.project.ProjectStructureUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Edit Project Channel screen.
 *
 * @param isLoading True if data is currently being loaded or saved.
 * @param initialChannelName The original name of the channel being edited.
 * @param initialChannelOrder The original order of the channel being edited.
 * @param channelNameInput Current value in the channel name input field.
 * @param channelOrderInput Current value in the channel order input field (as String).
 * @param nameError Error message for the channel name input, if any.
 * @param orderError Error message for the channel order input, if any.
 * @param generalError General error message not specific to a field.
 * @param channelsInSameCategory List of channels in the same category, for order validation by UpdateProjectChannelUseCase.
 * @param isSaveAttempted True if a save operation has been attempted.
 */
data class EditChannelUiState(
    val isLoading: Boolean = false,
    val initialChannelName: String = "",
    val initialChannelOrder: Double = 0.0,
    val channelNameInput: String = "",
    val channelOrderInput: String = "",
    val nameError: String? = null,
    val orderError: String? = null,
    val generalError: String? = null,
    val channelsInSameCategory: List<ProjectChannel> = emptyList(),
    val isSaveAttempted: Boolean = false
)

/**
 * Sealed class for events sent from the ViewModel to the UI.
 */
sealed class EditChannelEvent {
    /**
     * Event indicating that saving the channel was successful.
     */
    object SaveSuccess : EditChannelEvent()

    /**
     * Event to show a snackbar message.
     * @param message The message to display.
     */
    data class ShowSnackbar(val message: String) : EditChannelEvent()
}

/**
 * ViewModel for the Project Channel Editing screen.
 *
 * Handles fetching channel details, validating inputs, and saving changes.
 * @property savedStateHandle Handle to access navigation arguments.
 * @property getProjectChannelUseCase Use case to fetch details of a specific channel.
 * @property getProjectAllCategoriesUseCase Use case to fetch all categories and their channels in a project (for validation context).
 * @property updateProjectChannelUseCase Use case to update a project channel.
 */
@HiltViewModel
class EditProjectChannelViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val projectChannelUseCaseProvider: ProjectChannelUseCaseProvider,
    private val projectStructureUseCaseProvider: ProjectStructureUseCaseProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditChannelUiState())
    val uiState: StateFlow<EditChannelUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditChannelEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val projectId: String? = savedStateHandle[AppRoutes.Project.ARG_PROJECT_ID]
    private val channelId: String? = savedStateHandle[AppRoutes.Project.ARG_CHANNEL_ID]

    // Store the original channel to pass to the update use case
    private var originalChannel: ProjectChannel? = null
    // Store the parent category's order for validation
    private var parentCategoryOrder: Double? = null

    // Provider를 통해 생성된 UseCase 그룹
    private val projectChannelUseCases =
        projectChannelUseCaseProvider.createForProject(projectId ?: "")
    private val projectStructureUseCases = projectId?.let {
        projectStructureUseCaseProvider.createForProject(it)
    }

    init {
        if (projectId != null && channelId != null) {
            loadChannelDetails(channelId)
        } else {
            _uiState.update { it.copy(isLoading = false, generalError = "Project or Channel ID is missing.") }
        }
    }

    /**
     * Loads the details of the channel to be edited.
     * @param projId The ID of the project.
     * @param chanId The ID of the channel.
     */
    private fun loadChannelDetails(chanId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = projectChannelUseCases.getProjectChannelUseCase(chanId).first()) {
                is CustomResult.Success -> {
                    val channel = result.data
                    originalChannel = channel // Store the original channel
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            initialChannelName = channel.channelName.value,
                            initialChannelOrder = channel.order.value,
                            channelNameInput = channel.channelName.value,
                            channelOrderInput = channel.order.value.toString(),
                            generalError = null
                        )
                    }
                    // After loading channel details, load the category context for validation
                    /**
                    channel.parentCategoryId?.let { catId ->
                        loadCategoryAndChannelContext(projId, catId)
                    }
                    **/
                }
                is CustomResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = result.error.message ?: "Failed to load channel details."
                        )
                    }
                }
                is CustomResult.Loading, is CustomResult.Initial, is CustomResult.Progress -> Unit // Do nothing
            }
        }
    }

    /**
     * Loads the parent category's order and all channels within that category.
     * This data is used for validation when updating the channel.
     * @param projId The ID of the project.
     * @param catId The ID of the category to which the current channel belongs.
     */
    private fun loadCategoryAndChannelContext(projId: String, catId: String) {
        viewModelScope.launch {
            projectStructureUseCases?.getProjectAllCategoriesUseCase?.invoke(projId)
                ?.onEach { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val categoryCollection = result.data
                        val parentCategory =
                            categoryCollection.find { it.category.id.value == catId }
                        val channels = parentCategory?.channels ?: emptyList()

                        parentCategoryOrder = parentCategory?.category?.order?.value
                        _uiState.update {
                            it.copy(channelsInSameCategory = channels)
                        }

                        // Optional: Add a warning if context could not be fully loaded
                        var errorUpdate = ""
                        if (parentCategoryOrder == null) {
                            errorUpdate += "\nWarning: Could not load parent category order for validation."
                        }
                        if (channels.isEmpty() && channelId != null) { // Only warn if not a new channel context and channels are expected
                            errorUpdate += "\nWarning: Could not load other channels in the category for order validation."
                        }
                        if (errorUpdate.isNotEmpty() && _uiState.value.generalError == null) {
                            _uiState.update { it.copy(generalError = (_uiState.value.generalError ?: "") + errorUpdate) }
                        }
                    }
                    is CustomResult.Failure -> {
                        val errorUpdate = "\nWarning: Could not load category context for validation. (${result.error.message})"
                        if (_uiState.value.generalError == null) {
                            _uiState.update { it.copy(generalError = (_uiState.value.generalError ?: "") + errorUpdate) }
                        }
                    }
                    is CustomResult.Loading, is CustomResult.Initial, is CustomResult.Progress -> Unit // Do nothing
                }
            }
        }
    }

    /**
     * Called when the channel name input changes.
     * @param newName The new name entered by the user.
     */
    fun onNameChange(newName: String) {
        _uiState.update { it.copy(channelNameInput = newName, nameError = null, isSaveAttempted = false) }
    }

    /**
     * Called when the channel order input changes.
     * @param newOrder The new order entered by the user (as a String).
     */
    fun onOrderChange(newOrder: String) {
        _uiState.update { it.copy(channelOrderInput = newOrder, orderError = null, isSaveAttempted = false) }
    }

    /**
     * Attempts to save the channel changes.
     * Performs validation before calling the update use case.
     */
    fun saveChannel() {
        val currentProjectId = projectId ?: return
        val currentChannelId = channelId ?: return
        val currentParentCategoryOrder = parentCategoryOrder ?: run {
            _uiState.update { it.copy(isLoading = false, generalError = "Parent category information is missing. Cannot save.") }
            viewModelScope.launch { _eventFlow.emit(EditChannelEvent.ShowSnackbar("Parent category info missing.")) }
            return
        }
        val currentOriginalChannel = originalChannel ?: return

        val nameInput = _uiState.value.channelNameInput
        val orderInputString = _uiState.value.channelOrderInput

        val orderInputDouble = orderInputString.toDoubleOrNull()
        if (orderInputDouble == null) {
            _uiState.update { it.copy(orderError = "Order must be a valid number (e.g., 1.01).", isLoading = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true, isSaveAttempted = true, generalError = null, nameError = null, orderError = null) }

        viewModelScope.launch {
            when (val result = projectChannelUseCases.updateProjectChannelUseCase(
                name = nameInput,
                order = orderInputDouble
            )) {
                is CustomResult.Success -> {
                    _eventFlow.emit(EditChannelEvent.SaveSuccess)
                }
                is CustomResult.Failure -> {
                    val errorMessage = result.error.message ?: "Failed to update channel."
                    if (errorMessage.contains("name", ignoreCase = true)) {
                        _uiState.update { it.copy(nameError = errorMessage) }
                    } else if (errorMessage.contains("order", ignoreCase = true)) {
                        _uiState.update { it.copy(orderError = errorMessage) }
                    } else {
                        _uiState.update { it.copy(generalError = errorMessage) }
                    }
                    _eventFlow.emit(EditChannelEvent.ShowSnackbar(errorMessage))
                }
                is CustomResult.Loading, is CustomResult.Initial, is CustomResult.Progress -> Unit // Do nothing
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
