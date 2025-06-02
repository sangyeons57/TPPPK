package com.example.feature_main.viewmodel

/**
 * Events for the Add DM User dialog.
 */
sealed interface AddDmUserEvent {
    data class NavigateToDmChat(val dmChannelId: String) : AddDmUserEvent
    data class ShowSnackbar(val message: String) : AddDmUserEvent
    object DismissDialog : AddDmUserEvent
}
