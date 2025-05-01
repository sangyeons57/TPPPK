// 경로: domain/model/ProjectStructure.kt (ProjectSettingViewModel 용, 선택적)
// 또는 Category, Channel 리스트를 개별적으로 반환받을 수도 있음
package com.example.teamnovapersonalprojectprojectingkotlin.domain.model

data class ProjectStructure(
    val categories: List<Category>,
    val channels: List<Channel> // 또는 Map<CategoryId, List<Channel>> 형태
)