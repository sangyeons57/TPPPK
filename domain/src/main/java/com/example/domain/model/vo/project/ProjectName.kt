package com.example.domain.model.vo.project

@JvmInline
value class ProjectName(val value: String) {

    fun isBlank(): Boolean {
        return value.isBlank()
    }

    fun trim(): ProjectName {
        return ProjectName(value.trim())
    }

    fun ifEmpty(default: () -> ProjectName): ProjectName {
        return if (isBlank()) default() else this
    }

    companion object {
        val EMPTY = ProjectName("")
    }
}
