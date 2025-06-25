package com.example.domain.model.vo.project

import java.text.DecimalFormat

@JvmInline
value class ProjectName(val value: String) {

    fun isBlank(): Boolean {
        return value.isBlank()
    }

    companion object {


    }
}
