package com.example.domain.model.collection

import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel


data class CategoryCollection(
    val category: Category,
    val channels: List<ProjectChannel> // ProjectChannelCollection 대신 ProjectChannel 기본 모델 리스트
)
