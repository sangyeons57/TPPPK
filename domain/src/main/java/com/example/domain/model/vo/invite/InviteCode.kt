package com.example.domain.model.vo.invite

@JvmInline
value class InviteCode(val value: String) {
    init {
        // Example validation: Ensure invite code has a specific length or format if necessary
        // require(value.length == 8) { "Invite code must be 8 characters long." } // Example
    }
}
