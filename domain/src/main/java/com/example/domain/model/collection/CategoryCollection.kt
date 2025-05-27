package com.example.domain.model.collection

import com.example.domain.model.Category
import com.example.domain.model.ProjectChannel // ProjectChannel 기본 모델 import

data class CategoryCollection(
    val category: Category,
    val channels: List<ProjectChannel>? = null // ProjectChannelCollection 대신 ProjectChannel 기본 모델 리스트
)
