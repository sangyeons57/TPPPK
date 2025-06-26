package com.example.domain.model.vo

import android.net.Uri
import java.net.URI

/**
 * Generic URL value object for images stored in Firebase Storage, web, etc.
 * All modules can reuse this for profile images, project thumbnails, etc.
 */
@JvmInline
value class ImageUrl(val value: String) {
    init {
//        require(value.isNotBlank()) { "ImageUrl must not be blank." }
//        require(value.length <= MAX_LENGTH) { "ImageUrl cannot exceed $MAX_LENGTH characters." }
//        require(value.matches(URL_REGEX)) { "Invalid image URL format." }
    }


    companion object {
        const val MAX_LENGTH = 500
        private val URL_REGEX = Regex("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$")

        fun toImageUrl(uri: URI): ImageUrl {
            return ImageUrl(uri.toString())
        }
        fun toImageUrl(uri: Uri): ImageUrl {
            return ImageUrl(uri.toString())
        }
    }
}
