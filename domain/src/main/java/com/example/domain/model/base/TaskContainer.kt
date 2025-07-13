package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import java.time.Instant

import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.task.TaskContainerOrder
import com.example.domain.model.vo.task.TaskContainerStatus

class TaskContainer private constructor(
    initialOrder: TaskContainerOrder,
    initialStatus: TaskContainerStatus,
    override val id: DocumentId,
    override var isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    // Mutable properties with private setters
    var order: TaskContainerOrder = initialOrder
        private set
    var status: TaskContainerStatus = initialStatus
        private set

    init {
        setOriginalState()
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_ORDER to this.order.value,
            KEY_STATUS to this.status.value,
            KEY_CREATED_AT to this.createdAt,
            KEY_UPDATED_AT to this.updatedAt
        )
    }

    /**
     * Updates the order of the task container.
     */
    fun updateOrder(newOrder: TaskContainerOrder) {
        if (this.order == newOrder) return
        this.order = newOrder
    }

    /**
     * Updates the status of the task container.
     */
    fun updateStatus(newStatus: TaskContainerStatus) {
        if (this.status == newStatus) return
        this.status = newStatus
    }

    /**
     * Archives the task container.
     */
    fun archive() {
        if (this.status == TaskContainerStatus.ARCHIVED) return
        this.status = TaskContainerStatus.ARCHIVED
    }

    /**
     * Activates the task container.
     */
    fun activate() {
        if (this.status == TaskContainerStatus.ACTIVE) return
        this.status = TaskContainerStatus.ACTIVE
    }

    /**
     * Marks the task container as deleted.
     */
    fun delete() {
        if (this.status == TaskContainerStatus.DELETED) return
        this.status = TaskContainerStatus.DELETED
    }

    companion object {
        const val COLLECTION_NAME = "task_container"
        const val KEY_ORDER = "order"
        const val KEY_STATUS = "status"
        
        /**
         * 고정된 TaskContainer ID
         * 통합된 collection에서 container 정의 문서는 항상 이 ID를 사용합니다.
         */
        val FIXED_CONTAINER_ID = DocumentId("container")

        /**
         * Factory method for creating a new task container.
         * 고정된 container ID를 사용합니다 (루트 레벨 컨테이너용).
         */
        fun create(
            order: TaskContainerOrder = TaskContainerOrder.DEFAULT
        ): TaskContainer {
            val taskContainer = TaskContainer(
                initialOrder = order,
                initialStatus = TaskContainerStatus.ACTIVE,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                id = FIXED_CONTAINER_ID,
                isNew = true
            )
            return taskContainer
        }
        
        /**
         * Factory method for creating a new nested task container.
         * 중첩된 TaskContainer의 경우 사용자 정의 ID를 사용할 수 있습니다.
         */
        fun createNested(
            id: DocumentId,
            order: TaskContainerOrder = TaskContainerOrder.DEFAULT
        ): TaskContainer {
            val taskContainer = TaskContainer(
                initialOrder = order,
                initialStatus = TaskContainerStatus.ACTIVE,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                id = id,
                isNew = true
            )
            return taskContainer
        }
        
        /**
         * Factory method for creating a new task container with custom id (deprecated).
         * @deprecated Use create() for root containers or createNested() for nested containers instead
         */
        @Deprecated("Use create() for root containers or createNested() for nested containers instead")
        fun create(
            id: DocumentId,
            order: TaskContainerOrder = TaskContainerOrder.DEFAULT
        ): TaskContainer {
            return createNested(id, order)
        }

        /**
         * Factory method to reconstitute a TaskContainer from a data source.
         */
        fun fromDataSource(
            id: DocumentId,
            order: TaskContainerOrder,
            status: TaskContainerStatus,
            createdAt: Instant?,
            updatedAt: Instant?
        ): TaskContainer {
            return TaskContainer(
                initialOrder = order,
                initialStatus = status,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                id = id,
                isNew = false
            )
        }
    }
}