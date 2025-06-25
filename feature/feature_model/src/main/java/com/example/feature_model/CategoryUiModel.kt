package com.example.feature_model

import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.category.CategoryName

data class CategoryUiModel(
    val id: DocumentId,
    val name: CategoryName,
    val channels: List<ChannelUiModel>
)
