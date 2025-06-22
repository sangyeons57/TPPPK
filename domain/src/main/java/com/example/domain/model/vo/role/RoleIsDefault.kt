package com.example.domain.model.vo.role

import kotlin.jvm.JvmInline

@JvmInline
value class RoleIsDefault(val value: Boolean)  {
    companion object {

    }
    fun isDefault () : Boolean = value
}