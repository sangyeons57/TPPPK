package com.example.feature_task.model

import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.task.TaskType
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder
import com.example.domain.model.vo.UserId
import java.time.Instant

/**
 * Task.kt domain 모델을 ui에서 사용할있도록 모델을 만듦
 * 
 * Google Keep 스타일의 간단한 메모 리스트를 위한 UI 모델
 */

data class TaskUiModel(
    val id: DocumentId,
    val taskType: TaskType,
    val status: TaskStatus,
    val content: TaskContent,
    val order: TaskOrder,
    val checkedBy: UserId?,
    val checkedAt: Instant?,
    val updatedAt: Instant
) {
    
    /**
     * Helper method to get display title from content
     */
    val title: String
        get() = content.value.split("\n").firstOrNull() ?: ""
    
    /**
     * Helper method to get display description from content
     */
    val description: String
        get() = content.value.split("\n").drop(1).joinToString("\n").takeIf { it.isNotBlank() } ?: ""
    
    /**
     * Helper method to check if task is completed
     * 체크박스 타입의 경우 TaskType으로 체크 상태를 판단
     */
    val isCompleted: Boolean
        get() = when {
            taskType.isCheckbox() -> taskType.isChecked()
            else -> status == TaskStatus.COMPLETED
        }
}