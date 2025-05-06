package com.example.feature_chat.model

import android.net.Uri

/**
 * 갤러리 이미지 UI 표시를 위한 데이터 모델
 */
data class GalleryImageUiModel(
    val uri: Uri,
    val id: String,
    var isSelected: Boolean = false // 선택 상태 추가 (UI 관리용)
) 