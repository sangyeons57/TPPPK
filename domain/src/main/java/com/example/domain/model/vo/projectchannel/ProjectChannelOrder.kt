package com.example.domain.model.vo.projectchannel

import kotlin.jvm.JvmInline

@JvmInline
value class ProjectChannelOrder(val value: Double) {
    init {
        require(value > 0) { "ProjectChannelOrder must be a positive number." }
    }
}
