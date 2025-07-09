package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import java.time.Instant

import com.example.domain.model.AggregateRoot
import com.example.domain.event.project.ProjectCreatedEvent
import com.example.domain.event.project.ProjectImageUrlChangedEvent
import com.example.domain.event.project.ProjectNameChangedEvent
import com.example.domain.event.project.ProjectStatusChangedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.model.vo.project.ProjectStatus

class Project private constructor(
    initialName: ProjectName,
    initialImageUrl: ImageUrl?,
    initialOwnerId: OwnerId,
    initialStatus: ProjectStatus,
    override val isNew: Boolean,
    override val id: DocumentId,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    // Immutable properties
    val ownerId: OwnerId = initialOwnerId

    // Mutable properties with private setters
    var name: ProjectName = initialName
        private set
    @Deprecated("Use fixed path system with project_profiles/{projectId}/profile.webp instead")
    var imageUrl: ImageUrl? = initialImageUrl
        private set
    var status: ProjectStatus = initialStatus
        private set

    init {
        setOriginalState()
    }

    // Implementation of abstract method from AggregateRoot
    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_NAME to this.name.value,
            KEY_IMAGE_URL to this.imageUrl?.value,
            KEY_STATUS to this.status.value,
            KEY_CREATED_AT to this.createdAt,
            KEY_UPDATED_AT to this.updatedAt,
            KEY_OWNER_ID to this.ownerId.value
        )
    }

    /**
     * Changes the name of the project, firing a domain event if the name is different.
     */
    fun changeName(newName: ProjectName) {
        if (this.name == newName) return

        val oldName = this.name
        this.name = newName
        pushDomainEvent(ProjectNameChangedEvent(this.id, oldName, newName, DateTimeUtil.nowInstant()))
    }

    /**
     * Changes the image URL of the project, firing a domain event.
     * 
     * @deprecated Use fixed path system with project_profiles/{projectId}/profile.webp instead.
     * This method is kept for backward compatibility but should not be used for new implementations.
     */
    @Deprecated("Use fixed path system with project_profiles/{projectId}/profile.webp instead")
    fun changeImageUrl(newImageUrl: ImageUrl?) {
        if (this.imageUrl == newImageUrl) return

        this.imageUrl = newImageUrl
        pushDomainEvent(ProjectImageUrlChangedEvent(this.id, newImageUrl, DateTimeUtil.nowInstant()))
    }

    /**
     * Marks the project as deleted (soft delete).
     */
    fun delete() {
        if (this.status == ProjectStatus.DELETED) return

        val oldStatus = this.status
        this.status = ProjectStatus.DELETED
        pushDomainEvent(ProjectStatusChangedEvent(this.id, oldStatus, ProjectStatus.DELETED, DateTimeUtil.nowInstant()))
    }

    /**
     * Archives the project.
     */
    fun archive() {
        if (this.status == ProjectStatus.ARCHIVED) return

        val oldStatus = this.status
        this.status = ProjectStatus.ARCHIVED
        pushDomainEvent(ProjectStatusChangedEvent(this.id, oldStatus, ProjectStatus.ARCHIVED, DateTimeUtil.nowInstant()))
    }

    /**
     * Activates the project.
     */
    fun activate() {
        if (this.status == ProjectStatus.ACTIVE) return

        val oldStatus = this.status
        this.status = ProjectStatus.ACTIVE
        pushDomainEvent(ProjectStatusChangedEvent(this.id, oldStatus, ProjectStatus.ACTIVE, DateTimeUtil.nowInstant()))
    }

    companion object {
        const val COLLECTION_NAME = "projects"
        const val KEY_NAME = "name"
        const val KEY_IMAGE_URL = "imageUrl"
        const val KEY_STATUS = "status"
        const val KEY_OWNER_ID = "ownerId"
        /**
         * Factory method to create a new Project.
         */
        fun create(
            name: ProjectName,
            imageUrl: ImageUrl?,
            ownerId: OwnerId
        ): Project {
            val project = Project(
                initialName = name,
                initialImageUrl = imageUrl,
                initialOwnerId = ownerId,
                initialStatus = ProjectStatus.ACTIVE,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                isNew = true,
                id = DocumentId.EMPTY
            )
            return project
        }

        /**
         * Factory method to reconstitute a Project from a data source.
         */
        fun fromDataSource(
            id: DocumentId,
            name: ProjectName,
            imageUrl: ImageUrl?,
            ownerId: OwnerId,
            status: ProjectStatus?,
            createdAt: Instant?,
            updatedAt: Instant?
        ): Project {
            return Project(
                initialName = name,
                initialImageUrl = imageUrl,
                initialOwnerId = ownerId,
                initialStatus = status ?: ProjectStatus.ACTIVE,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                id = id,
                isNew = false,
            )
        }
    }
}

