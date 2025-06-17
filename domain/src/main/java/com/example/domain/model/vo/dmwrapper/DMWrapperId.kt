package com.example.domain.model.vo.dmwrapper

import kotlin.jvm.JvmInline

@JvmInline
value class DMWrapperId(val value: String) {
    init {
        require(value.isNotBlank()) { "DMWrapperId cannot be blank." }
    }
}
