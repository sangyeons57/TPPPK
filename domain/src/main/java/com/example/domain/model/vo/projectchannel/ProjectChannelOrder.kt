package com.example.domain.model.vo.projectchannel

import java.text.DecimalFormat

@JvmInline
value class ProjectChannelOrder(val value: Int) {
    init {
        require(value >= 0) { "ProjectChannelOrder must be non-negative" }
    }

    companion object {
        val DEFAULT = ProjectChannelOrder(0)

        fun from(raw: Int): ProjectChannelOrder {
            return ProjectChannelOrder(raw)
        }
        
        fun fromDouble(raw: Double): ProjectChannelOrder {
            return ProjectChannelOrder(raw.toInt())
        }
    }
}
