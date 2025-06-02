package com.example.feature_project.model

data class CategoryUiModel(
    val id: String,
    val name: String,
    val channels: List<ChannelUiModel>
)
