package com.example.domain.model.vo.message

@JvmInline
value class MessageContent(val value: String)  {
    init {
        require(value.isNotEmpty()) { "MessageContent cannot be empty." }
    }
}