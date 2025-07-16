package com.example.domain.model.base

import com.example.core_common.util.DateTimeUtil
import java.time.Instant

import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskType

class Task private constructor(
    initialTaskType: TaskType,
    initialStatus: TaskStatus,
    initialContent: TaskContent,
    initialOrder: TaskOrder,
    override val id: DocumentId,
    override var isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : AggregateRoot() {

    // Mutable properties with private setters
    var taskType: TaskType = initialTaskType
        private set
    var status: TaskStatus = initialStatus
        private set
    var content: TaskContent = initialContent
        private set
    var order: TaskOrder = initialOrder
        private set

    init {
        setOriginalState()
    }

    override fun getCurrentStateMap(): Map<String, Any?> {
        return mapOf(
            KEY_TASK_TYPE to this.taskType.value,
            KEY_STATUS to this.status.value,
            KEY_CONTENT to this.content.value,
            KEY_ORDER to this.order.value,
            KEY_CREATED_AT to this.createdAt,
            KEY_UPDATED_AT to this.updatedAt
        )
    }

    /**
     * Updates the task type.
     */
    fun updateTaskType(newTaskType: TaskType) {
        if (this.taskType == newTaskType) return
        this.taskType = newTaskType
    }

    /**
     * Updates the status of the task.
     */
    fun updateStatus(newStatus: TaskStatus) {
        if (this.status == newStatus) return
        this.status = newStatus
    }

    /**
     * Updates the content of the task.
     */
    fun updateContent(newContent: TaskContent) {
        if (this.content == newContent) return
        this.content = newContent
    }

    /**
     * Updates the order of the task.
     */
    fun updateOrder(newOrder: TaskOrder) {
        if (this.order == newOrder) return
        this.order = newOrder
    }

    /**
     * Marks the task as completed.
     */
    fun complete() {
        if (this.status == TaskStatus.COMPLETED) return
        this.status = TaskStatus.COMPLETED
    }

    /**
     * Marks the task as in progress.
     */
    fun startProgress() {
        if (this.status == TaskStatus.IN_PROGRESS) return
        this.status = TaskStatus.IN_PROGRESS
    }

    /**
     * Marks the task as pending.
     */
    fun markAsPending() {
        if (this.status == TaskStatus.PENDING) return
        this.status = TaskStatus.PENDING
    }

    companion object {
        const val COLLECTION_NAME = "task_container"  // 통합된 collection 이름
        const val KEY_TASK_TYPE = "taskType"
        const val KEY_STATUS = "status"
        const val KEY_CONTENT = "content"
        const val KEY_ORDER = "order"

        /**
         * Factory method for creating a new task.
         */
        fun create(
            id: DocumentId,
            taskType: TaskType = TaskType.CHECKLIST,
            content: TaskContent = TaskContent.EMPTY,
            order: TaskOrder = TaskOrder.DEFAULT
        ): Task {
            val task = Task(
                initialTaskType = taskType,
                initialStatus = TaskStatus.PENDING,
                initialContent = content,
                initialOrder = order,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                id = id,
                isNew = true
            )
            return task
        }

        /**
         * Factory method to reconstitute a Task from a data source.
         */
        fun fromDataSource(
            id: DocumentId,
            taskType: TaskType,
            status: TaskStatus,
            content: TaskContent,
            order: TaskOrder,
            createdAt: Instant?,
            updatedAt: Instant?
        ): Task {
            return Task(
                initialTaskType = taskType,
                initialStatus = status,
                initialContent = content,
                initialOrder = order,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                id = id,
                isNew = false
            )
        }
    }
}