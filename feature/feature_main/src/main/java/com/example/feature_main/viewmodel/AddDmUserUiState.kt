package com.example.feature_main.viewmodel

/**
 * UI state for the Add DM User dialog.
 */
data class AddDmUserUiState(
    val username: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
    // Potentially: val searchResults: List<SomeUserSearchUiModel> = emptyList()
)
