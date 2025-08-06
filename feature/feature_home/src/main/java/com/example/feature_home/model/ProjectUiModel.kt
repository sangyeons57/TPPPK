package com.example.feature_home.model // Adjusted package

import com.example.domain.model.base.Project
import com.example.domain.vo.DocumentId
import com.example.domain.vo.ImageUrl
import com.example.domain.vo.project.ProjectName

/**
 * UI data model for a Project item in a list.
 */
data class ProjectUiModel(
    val id: DocumentId,
    val name: ProjectName,
    val imageUrl: ImageUrl?, // For project cover image
) {
    // Companion object for creating preview instances easily
    companion object {
        fun preview(): ProjectUiModel {
            return ProjectUiModel(
                id = DocumentId("project_preview_id_abc"),
                name = ProjectName("Projecting Kotlin 앱 개발"),
                imageUrl = null, // Or a placeholder image URL
            )
        }

        fun emptyPreviewList(): List<ProjectUiModel> = listOf(
            ProjectUiModel(
                id = DocumentId("proj_1"),
                name = ProjectName("알고리즘 스터디"),
                imageUrl = null,
            ),
            ProjectUiModel(
                id = DocumentId("proj_2"),
                name = ProjectName("사이드 프로젝트: 맛집 앱"),
                imageUrl = null,
            )
        )
    }
}

/**
 * Maps a Project domain model to a ProjectUiModel.
 */
fun Project.toProjectUiModel(): ProjectUiModel {
    return ProjectUiModel(
        id = this.id,
        name = this.name,
        imageUrl = ImageUrl("") //프로젝트 이미지는 사용자 이미지와 같이 id를 사용한 경로 고정으로 동작한다.따라서 여기 로직 고쳐야함 project_profile/{project_id}/profile.webp
    )
}

/**
 * Maps a ProjectsWrapper domain model to a ProjectUiModel.
 */
fun com.example.domain.model.base.ProjectsWrapper.toProjectUiModel(): ProjectUiModel {
    return ProjectUiModel(
        id = this.id,
        name = this.projectName,
        imageUrl = this.projectImageUrl
    )
}
