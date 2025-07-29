package com.example.domain.usecase.local.project.structure

import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId

data class ProjectStructure(
    val categories: List<Category>,
    val channels: Map<DocumentId, List<ProjectChannel>>
) 