package com.example.domain.model.vo.invite

import com.example.domain.model.vo.DocumentId // Assuming DocumentId is a suitable wrapper for generic IDs

@JvmInline
value class InviteId(val value: String) {
    init {
        require(value.isNotBlank()) { "Invite ID cannot be blank." }
    }
}
