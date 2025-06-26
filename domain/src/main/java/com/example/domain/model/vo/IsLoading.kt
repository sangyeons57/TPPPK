package com.example.domain.model.vo

/**
 * Generic URL value object for images stored in Firebase Storage, web, etc.
 * All modules can reuse this for profile images, project thumbnails, etc.
 */
@JvmInline
value class IsLoading(val value: Boolean) {
    init {
    }


    companion object {
        val True = IsLoading(true)
        val False = IsLoading(false)
    }

    fun reverse() = IsLoading(!value)
    fun isEnable(): Boolean = !value
}
