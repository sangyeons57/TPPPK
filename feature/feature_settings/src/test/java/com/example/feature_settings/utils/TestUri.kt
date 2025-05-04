package com.example.feature_settings.utils

import android.net.Uri

/**
 * 테스트용 Uri 구현체
 * 
 * 테스트에서 Uri를 쉽게 사용할 수 있도록 만든 간단한 구현체입니다.
 */
class TestUri(private val path: String) : Uri() {
    override fun toString(): String = "content://test/$path"
    
    override fun getScheme(): String? = "content"
    override fun getEncodedPath(): String? = path
    override fun getPath(): String? = path
    override fun getLastPathSegment(): String? = path.substringAfterLast('/')
    override fun getAuthority(): String? = "test"
    override fun getQuery(): String? = null
    override fun getHost(): String? = "test"
    override fun getPort(): Int = -1
    override fun getFragment(): String? = null
    override fun getUserInfo(): String? = null
    override fun buildUpon(): Builder = Uri.Builder()
    override fun isHierarchical(): Boolean = true
    override fun isRelative(): Boolean = false
    override fun isAbsolute(): Boolean = true
    override fun isOpaque(): Boolean = false
    override fun getEncodedUserInfo(): String? = null
    override fun getEncodedAuthority(): String? = "test"
    override fun getEncodedFragment(): String? = null
    override fun getSchemeSpecificPart(): String? = "//test/$path"
    override fun getEncodedSchemeSpecificPart(): String? = "//test/$path"
} 