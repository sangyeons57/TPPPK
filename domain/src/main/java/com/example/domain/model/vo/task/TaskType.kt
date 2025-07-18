package com.example.domain.model.vo.task

/**
 * Enum representing the type of a task.
 */
enum class TaskType(val value: String) {
    CHECKLIST("checklist"),
    CHECKLIST_CHECKED("checklist_checked"),
    COMMENT("comment");

    /**
     * 체크박스 타입인지 확인
     */
    fun isCheckbox(): Boolean = this == CHECKLIST || this == CHECKLIST_CHECKED
    
    /**
     * 체크된 체크박스인지 확인
     */
    fun isChecked(): Boolean = this == CHECKLIST_CHECKED
    
    /**
     * 체크 상태를 토글한 타입 반환
     */
    fun toggleChecked(): TaskType = when (this) {
        CHECKLIST -> CHECKLIST_CHECKED
        CHECKLIST_CHECKED -> CHECKLIST
        COMMENT -> this // 메모는 토글하지 않음
    }

    companion object {
        fun fromValue(value: String): TaskType {
            return values().find { it.value == value } 
                ?: throw IllegalArgumentException("Unknown TaskType value: $value")
        }
    }
}