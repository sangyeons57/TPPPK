package com.example.domain.model.vo.role

@JvmInline
value class RoleIsDefault(val value: Boolean)  {
    companion object {
        val DEFAULT = RoleIsDefault(true)
        val NON_DEFAULT = RoleIsDefault(false)
    }
    fun isDefault () : Boolean = value
}