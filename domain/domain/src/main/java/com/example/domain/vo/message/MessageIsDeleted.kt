package com.example.domain.model.vo.message


@JvmInline
value class MessageIsDeleted(val value: Boolean) {
    companion object {
        val TRUE = MessageIsDeleted(true)
        val FALSE = MessageIsDeleted(false)
    }
}