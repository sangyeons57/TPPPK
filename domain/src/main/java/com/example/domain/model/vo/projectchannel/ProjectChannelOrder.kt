package com.example.domain.model.vo.projectchannel

import java.text.DecimalFormat

@JvmInline
value class ProjectChannelOrder(val value: Double) {
    init {
//        require(value > 0) { "ProjectChannelOrder must be a positive number." }
//        require(isTwoDecimalPlace(value)) { "ProjectChannelOrder must have at most two decimal places (00.00 format)." }
    }

    companion object {
        val DEFAULT = ProjectChannelOrder(com.example.domain.model.base.Category.NO_CATEGORY_ORDER)

        private fun formatting (v: Double): Double = DecimalFormat("0.00").format(v).toDouble()

        private fun isTwoDecimalPlace(v: Double): Boolean {
            // Convert to string with 2 decimal places and compare after toDouble()
            return formatting(v) == v
        }

        fun from(raw: Double): ProjectChannelOrder {
            return ProjectChannelOrder(formatting(raw))
        }

        fun from(raw: Int): ProjectChannelOrder {
            return ProjectChannelOrder(raw.toDouble())
        }
    }
}
