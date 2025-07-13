package com.example.domain.event.task

import com.example.domain.event.DomainEvent
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.task.TaskTitle
import com.example.domain.model.vo.task.TaskDescription
import java.time.Instant

/**
 * Event indicating that a task has been updated.
 */
data class TaskUpdatedEvent(
    val taskId: DocumentId,
    val oldTitle: TaskTitle?,
    val newTitle: TaskTitle?,
    val oldDescription: TaskDescription?,
    val newDescription: TaskDescription?,
    override val occurredOn: Instant
) : DomainEvent