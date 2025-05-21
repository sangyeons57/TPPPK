package com.example.domain.model.project

/**
 * Enum class for specifying sorting options for project roles.
 */
enum class RoleSortOption {
    NAME_ASC,
    NAME_DESC
    // Add MEMBER_COUNT_ASC, MEMBER_COUNT_DESC later if Role.memberCount becomes reliably sortable.
}
