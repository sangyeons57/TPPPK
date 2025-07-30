package com.example.domain.event.task

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskType
import java.time.Instant

/**
 * Event indicating that a task has been updated.
 */
data class TaskUpdatedEvent(
    val taskId: DocumentId,
    val oldTaskType: TaskType?,
    val newTaskType: TaskType?,
    val oldContent: TaskContent?,
    val newContent: TaskContent?,
    override val occurredOn: Instant
) : DomainEvent