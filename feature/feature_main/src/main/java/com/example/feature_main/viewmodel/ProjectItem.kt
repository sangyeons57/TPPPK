package com.example.feature_main.viewmodel

/**
 * Represents a project item in the ViewModel, often used for UI state or previews.
 * This class is used to hold basic project information.
 */
data class ProjectItem(
    val id: String,
    val name: String,
    val description: String?,
    val lastActivity: String?
)
