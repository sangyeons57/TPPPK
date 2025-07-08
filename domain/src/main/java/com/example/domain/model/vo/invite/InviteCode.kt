package com.example.domain.model.vo.invite

@JvmInline
value class InviteCode(val value: String) {
    init {
        require(value.isNotBlank()) { "Invite code cannot be blank" }
        require(value.length >= 6) { "Invite code must be at least 6 characters long" }
        require(value.length <= 32) { "Invite code must be at most 32 characters long" }
        require(value.all { it.isLetterOrDigit() }) { "Invite code must contain only letters and numbers" }
    }
    
    companion object {
        /**
         * Creates an InviteCode from a DocumentId
         */
        fun from(documentId: com.example.domain.model.vo.DocumentId): InviteCode {
            return InviteCode(documentId.value)
        }
    }
}
