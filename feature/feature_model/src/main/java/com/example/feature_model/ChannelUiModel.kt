package com.example.feature_model

import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.projectchannel.ProjectChannelOrder

// Assuming ProjectChannelType is available or will be defined/imported
// For now, let's use a String for channelType as ProjectChannel domain model has ProjectChannelType enum
// import com.example.domain.model.enum.ProjectChannelType

data class ChannelUiModel(
    val id: DocumentId,
    val name: Name,
    val categoryId: DocumentId, // Null if it's a direct channel
    val isDirect: Boolean, // True if it's a direct channel (not under a category)
    val channelType: ProjectChannelType, // e.g., "MESSAGES", "TASKS". Domain model uses ProjectChannelType enum.
                                  // Consider mapping to a UI-specific enum or String for simplicity here.
    val order: ProjectChannelOrder // Order for sorting channels
)
