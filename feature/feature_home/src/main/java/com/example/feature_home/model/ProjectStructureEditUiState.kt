package com.example.feature_home.model


/**
 * UI state for the Project Structure Edit dialog.
 */
data class ProjectStructureEditUiState(
    val projectStructure: ProjectStructureUiState = ProjectStructureUiState(), // The data being edited
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
