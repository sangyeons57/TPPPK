package com.example.feature_main.ui // Adjusted package

import java.util.Date // Using java.util.Date as a placeholder

/**
 * UI data model for a Project item in a list.
 */
data class ProjectUiModel(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?, // For project cover image
    val memberCount: Int = 0, // Example: useful for project lists
    val lastActivity: String? = "어제" // Example: "어제", "2시간 전"
) {
    // Companion object for creating preview instances easily
    companion object {
        fun preview(): ProjectUiModel {
            return ProjectUiModel(
                id = "project_preview_id_abc",
                name = "Projecting Kotlin 앱 개발",
                description = "Clean Architecture와 MVVM을 적용한 안드로이드 앱 개발 프로젝트입니다. 다양한 기능을 구현하고 테스트합니다.",
                imageUrl = null, // Or a placeholder image URL
                memberCount = 5,
                lastActivity = "1시간 전"
            )
        }

        fun emptyPreviewList(): List<ProjectUiModel> = listOf(
            ProjectUiModel(
                id = "proj_1",
                name = "알고리즘 스터디",
                description = "매주 코딩 테스트 문제 풀이",
                imageUrl = null,
                memberCount = 4,
                lastActivity = "3일 전"
            ),
            ProjectUiModel(
                id = "proj_2",
                name = "사이드 프로젝트: 맛집 앱",
                description = "나만의 맛집 지도 만들기",
                imageUrl = null,
                memberCount = 3,
                lastActivity = "오늘"
            )
        )
    }
}
