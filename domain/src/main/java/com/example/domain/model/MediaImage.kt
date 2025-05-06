package com.example.domain.model

import android.net.Uri

/**
 * 미디어 이미지 정보를 나타내는 도메인 모델
 * 안드로이드 의존성 없이 순수한 문자열 경로 사용 -> Uri 사용으로 변경
 */
data class MediaImage(
    val id: String,
    val contentPath: Uri, // String -> Uri 변경
    val name: String = "",
    val size: Long = 0L,
    val mimeType: String = "",
    val dateAdded: Long = 0L
)