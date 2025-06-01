package com.example.domain.model.ui.project

/**
 * Enum representing the available sorting options for project roles.
 * 이 모델은 UI 계층에서 역할 정렬 옵션을 표시하는데 사용됩니다.
 */
enum class RoleSortOption {
    /** Sort roles by name in ascending order (A-Z) */
    NAME_ASC,
    
    /** Sort roles by name in descending order (Z-A) */
    NAME_DESC,
    
    /** Sort roles by creation date in ascending order (oldest first) */
    CREATED_AT_ASC,
    
    /** Sort roles by creation date in descending order (newest first) */
    CREATED_AT_DESC
}
