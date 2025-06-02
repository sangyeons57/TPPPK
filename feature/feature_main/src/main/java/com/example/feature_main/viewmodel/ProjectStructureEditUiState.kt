package com.example.feature_main.viewmodel

import com.example.feature_main.ui.project.ProjectStructureUiState


/**
 * UI state for the Project Structure Edit dialog.
 */
data class ProjectStructureEditUiState(
    val projectStructure: ProjectStructureUiState = ProjectStructureUiState(), // The data being edited
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
