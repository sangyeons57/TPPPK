package com.example.teamnovapersonalprojectprojectingkotlin.domain.model

import android.net.Uri // 플랫폼 종속성 고려 필요

data class MediaImage(
    val id: Long, // MediaStore ID 등 고유 식별자
    val contentUri: Uri, // 이미지의 Content URI
    val displayName: String? = null // 파일 이름 (선택적)
)