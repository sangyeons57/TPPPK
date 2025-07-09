package com.example.domain.model.vo

/**
 * Represents a URL pointing to an image. This is a lightweight wrapper maintained to avoid
 * widespread refactors after removing profileImageUrl from User. Other domain entities (e.g.,
 * Project, Friend) still rely on this value object.
 */
@JvmInline
value class ImageUrl(val value: String) {
    override fun toString(): String = value
} 