package com.example.feature_model

data class CategoryUiModel(
    val id: String,
    val name: String,
    val channels: List<ChannelUiModel>
)
