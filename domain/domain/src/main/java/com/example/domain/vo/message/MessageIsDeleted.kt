package com.example.domain.vo.message


@JvmInline
value class MessageIsDeleted(val value: Boolean) {
    companion object {
        val TRUE = MessageIsDeleted(true)
        val FALSE = MessageIsDeleted(false)

        fun fromBoolean(value: Boolean): MessageIsDeleted {
            return if (value) TRUE else FALSE
        }
    }
}