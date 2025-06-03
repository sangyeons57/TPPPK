package com.example.feature_main.ui // Adjusted package

import com.example.domain.model.base.ProjectsWrapper // Import ProjectsWrapper
import java.util.Date // Using java.util.Date as a placeholder

/**
 * UI data model for a Project item in a list.
 */
data class ProjectUiModel(
    val id: String,
    val name: String,
    val imageUrl: String?, // For project cover image
) {
    // Companion object for creating preview instances easily
    companion object {
        fun preview(): ProjectUiModel {
            return ProjectUiModel(
                id = "project_preview_id_abc",
                name = "Projecting Kotlin 앱 개발",
                imageUrl = null, // Or a placeholder image URL
            )
        }

        fun emptyPreviewList(): List<ProjectUiModel> = listOf(
            ProjectUiModel(
                id = "proj_1",
                name = "알고리즘 스터디",
                imageUrl = null,
            ),
            ProjectUiModel(
                id = "proj_2",
                name = "사이드 프로젝트: 맛집 앱",
                imageUrl = null,
            )
        )
    }
}

/**
 * Maps a ProjectsWrapper domain model to a ProjectUiModel.
 */
fun ProjectsWrapper.toProjectUiModel(): ProjectUiModel {
    return ProjectUiModel(
        id = this.projectId,
        name = this.projectName,
        imageUrl = this.projectImageUrl
    )
}
