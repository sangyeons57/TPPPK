package com.example.domain.model.base

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant

import com.example.domain.event.AggregateRoot
import com.example.domain.event.project.ProjectCreatedEvent
import com.example.domain.event.project.ProjectImageUrlChangedEvent
import com.example.domain.event.project.ProjectNameChangedEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.project.ProjectName

class Project private constructor(
    initialName: ProjectName,
    initialImageUrl: ImageUrl?,
    initialOwnerId: OwnerId,
    initialCreatedAt: Instant,
    initialUpdatedAt: Instant,
    override val isNew: Boolean,
    override val id: DocumentId,
) : AggregateRoot() {

    // Immutable properties
    val ownerId: OwnerId = initialOwnerId
    val createdAt: Instant = initialCreatedAt

    // Mutable properties with private setters
    var name: ProjectName = initialName
        private set
    var imageUrl: ImageUrl? = initialImageUrl
        private set
    var updatedAt: Instant = initialUpdatedAt
        private set

    // Implementation of abstract method from AggregateRoot
    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_NAME to this.name,
            KEY_IMAGE_URL to this.imageUrl,
            KEY_CREATED_AT to this.createdAt,
            KEY_UPDATED_AT to this.updatedAt,
            KEY_OWNER_ID to this.ownerId
        )
    }

    /**
     * Changes the name of the project, firing a domain event if the name is different.
     */
    fun changeName(newName: ProjectName) {
        if (this.name == newName) return

        val oldName = this.name
        this.name = newName
        this.updatedAt = Instant.now()
        pushDomainEvent(ProjectNameChangedEvent(this.id, oldName, newName, this.updatedAt))
    }

    /**
     * Changes the image URL of the project, firing a domain event.
     */
    fun changeImageUrl(newImageUrl: ImageUrl?) {
        if (this.imageUrl == newImageUrl) return

        this.imageUrl = newImageUrl
        this.updatedAt = Instant.now()
        pushDomainEvent(ProjectImageUrlChangedEvent(this.id, newImageUrl, this.updatedAt))
    }

    companion object {
        const val COLLECTION_NAME = "projects"
        const val KEY_NAME = "name"
        const val KEY_IMAGE_URL = "imageUrl"
        const val KEY_OWNER_ID = "ownerId"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_UPDATED_AT = "updatedAt"
        /**
         * Factory method to create a new Project.
         */
        fun create(
            name: ProjectName,
            imageUrl: ImageUrl?,
            ownerId: OwnerId
        ): Project {
            val now = Instant.now()
            val project = Project(
                initialName = name,
                initialImageUrl = imageUrl,
                initialOwnerId = ownerId,
                initialCreatedAt = now,
                initialUpdatedAt = now,
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
            createdAt: Instant,
            updatedAt: Instant
        ): Project {
            return Project(
                initialName = name,
                initialImageUrl = imageUrl,
                initialOwnerId = ownerId,
                initialCreatedAt = createdAt,
                initialUpdatedAt = updatedAt,
                id = id,
                isNew = false,
            )
        }
    }
}

