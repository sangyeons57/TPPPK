package com.example.domain.model

/**
 * 갤러리 이미지 정보를 담는 모델 클래스
 */
data class MediaImage(
    val id: Long, // MediaStore ID 등 고유 식별자
    val contentUri: String, // 이미지의 Content URI (문자열로 저장)
    val displayName: String? = null, // 파일 이름 (선택적)
    val size: Long = 0 // 파일 크기 (바이트)
)