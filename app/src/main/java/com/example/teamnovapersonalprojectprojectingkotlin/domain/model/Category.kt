// 경로: domain/model/Category.kt (ProjectSettingViewModel, ProjectStructure 관련 기반)
package com.example.teamnovapersonalprojectprojectingkotlin.domain.model

data class Category(
    val id: String,
    val projectId: String,
    val name: String,
    val order: Int // 카테고리 순서
)