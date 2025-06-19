package com.example.domain.model.base

import com.example.domain.event.AggregateRoot
import com.example.domain.event.projectwrapper.ProjectWrapperCreatedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.projectwrapper.ProjectWrapperOrder
import java.time.Instant

class ProjectsWrapper(
    val initialOrder: ProjectWrapperOrder,
    val initialCreatedAt: Instant,
    val initialUpdatedAt: Instant,
    override val id: DocumentId,
    override val isNew: Boolean,
) : AggregateRoot() {
    val createdAt: Instant = initialCreatedAt

    var order: ProjectWrapperOrder = initialOrder
        private set
    var updatedAt: Instant = initialUpdatedAt
        private set

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_ORDER to this.order,
            KEY_CREATED_AT to this.createdAt,
            KEY_UPDATED_AT to this.updatedAt
        )
    }

    companion object {
        const val COLLECTION_NAME = "projectsWrapper"
        const val KEY_ORDER = "order"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"

        fun create(id: DocumentId, order: ProjectWrapperOrder): ProjectsWrapper {
            val now = Instant.now()
            val projectsWrapper = ProjectsWrapper(
                initialOrder = order,
                initialCreatedAt = now,
                initialUpdatedAt = now,
                id = id,
                isNew = true
            )
            projectsWrapper.pushDomainEvent(
                ProjectWrapperCreatedEvent(
                    id = projectsWrapper.id,
                    occurredOn = now,
                    order = projectsWrapper.order
                )
            )
            return projectsWrapper
        }

        fun fromDataSource(
            id: DocumentId,
            order: ProjectWrapperOrder,
            createdAt: Instant,
            updatedAt: Instant
        ): ProjectsWrapper {
            return ProjectsWrapper(
                initialOrder = order,
                initialCreatedAt = createdAt,
                initialUpdatedAt = updatedAt,
                id = id,
                isNew = false
            )
        }
    }
}

