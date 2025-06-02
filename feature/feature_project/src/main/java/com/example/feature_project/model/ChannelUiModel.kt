package com.example.feature_project.model

// Assuming ProjectChannelType is available or will be defined/imported
// For now, let's use a String for channelType as ProjectChannel domain model has ProjectChannelType enum
// import com.example.domain.model.enum.ProjectChannelType

data class ChannelUiModel(
    val id: String,
    val name: String,
    val categoryId: String?, // Null if it's a direct channel
    val isDirect: Boolean, // True if it's a direct channel (not under a category)
    val channelType: String // e.g., "MESSAGES", "TASKS". Domain model uses ProjectChannelType enum.
                                  // Consider mapping to a UI-specific enum or String for simplicity here.
)
