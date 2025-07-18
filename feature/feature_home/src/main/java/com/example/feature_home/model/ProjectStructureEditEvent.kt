package com.example.feature_home.model

/**
 * Events for the Project Structure Edit dialog.
 */
sealed interface ProjectStructureEditEvent {
    data class ShowErrorSnackbar(val message: String) : ProjectStructureEditEvent
    data class ShowSuccessSnackbar(val message: String) : ProjectStructureEditEvent
    object DismissDialog : ProjectStructureEditEvent
    // Potentially: object StructureSaved : ProjectStructureEditEvent
}
