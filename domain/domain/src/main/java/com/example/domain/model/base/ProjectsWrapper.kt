package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import com.example.domain.AggregateRoot
import com.example.domain.vo.DocumentId
import com.example.domain.vo.ImageUrl
import com.example.domain.vo.project.ProjectName
import com.example.domain.vo.projectwrapper.ProjectWrapperOrder
import java.time.Instant

class ProjectsWrapper(
    val initialOrder: ProjectWrapperOrder,
    val initialProjectName: ProjectName,
    val initialProjectImageUrl: ImageUrl?,
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    var order: ProjectWrapperOrder = initialOrder
        private set
    var projectName: ProjectName = initialProjectName
        private set
    var projectImageUrl: ImageUrl? = initialProjectImageUrl
        private set

    init {
        setOriginalState()
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_ORDER to this.order.value,
            KEY_PROJECT_NAME to this.projectName.value,
            KEY_PROJECT_IMAGE_URL to this.projectImageUrl?.value,
            KEY_CREATED_AT to this.createdAt,
            KEY_UPDATED_AT to this.updatedAt
        )
    }

    companion object {
        const val COLLECTION_NAME = "projects_wrapper"
        const val KEY_ORDER = "order"
        const val KEY_PROJECT_NAME = "projectName"
        const val KEY_PROJECT_IMAGE_URL = "projectImageUrl"

        fun create(id: DocumentId, projectName: ProjectName): ProjectsWrapper {
            val projectsWrapper = ProjectsWrapper(
                initialOrder = ProjectWrapperOrder.CREATE,
                initialProjectName = projectName,
                initialProjectImageUrl = null,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                id = id,
                isNew = true
            )
            return projectsWrapper
        }

        fun fromDataSource(
            id: DocumentId,
            order: ProjectWrapperOrder,
            projectName: ProjectName,
            projectImageUrl: ImageUrl?,
            createdAt: Instant?,
            updatedAt: Instant?
        ): ProjectsWrapper {
            return ProjectsWrapper(
                initialOrder = order,
                initialProjectName = projectName,
                initialProjectImageUrl = projectImageUrl,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                id = id,
                isNew = false
            )
        }
    }
}

