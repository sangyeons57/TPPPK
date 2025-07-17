package com.example.feature_task.model

import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.task.TaskType
import com.example.domain.model.vo.task.TaskStatus
import com.example.domain.model.vo.task.TaskContent
import com.example.domain.model.vo.task.TaskOrder
import java.time.Instant

/**
 * Task.kt domain 모델을 ui에서 사용할있도록 모델을 만듦
 * 
 * 동기화 및 충돌 처리를 위한 추가 필드들:
 * - isEditing: 현재 편집 모드인지
 * - hasConflict: 다른 사용자가 수정하여 충돌이 발생했는지
 * - lastModified: 마지막 수정 시간
 * - originalContent: 충돌 감지를 위한 원본 컨텐츠
 * - isUpdating: 업데이트 진행 상태
 */

data class TaskUiModel(
    val id: DocumentId,
    val taskType: TaskType,
    val status: TaskStatus,
    val content: TaskContent,
    val order: TaskOrder,
    val isEditing: Boolean = false,
    val hasConflict: Boolean = false,
    val lastModified: Instant? = null,
    val originalContent: TaskContent? = null,
    val isUpdating: Boolean = false
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
     */
    val isCompleted: Boolean
        get() = status == TaskStatus.COMPLETED
    
    /**
     * Helper method to detect if content has changed from original
     */
    fun hasContentChanged(): Boolean {
        return originalContent != null && content != originalContent
    }
    
    /**
     * Helper method to create editing state
     */
    fun toEditingState(): TaskUiModel {
        return copy(
            isEditing = true,
            originalContent = content
        )
    }
    
    /**
     * Helper method to create conflict state
     */
    fun toConflictState(): TaskUiModel {
        return copy(
            hasConflict = true,
            isEditing = false
        )
    }
    
    /**
     * Helper method to create updating state
     */
    fun toUpdatingState(): TaskUiModel {
        return copy(
            isUpdating = true,
            hasConflict = false
        )
    }
    
    /**
     * Helper method to create normal state
     */
    fun toNormalState(): TaskUiModel {
        return copy(
            isEditing = false,
            hasConflict = false,
            isUpdating = false,
            originalContent = null
        )
    }
}