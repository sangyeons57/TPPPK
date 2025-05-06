package com.example.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 로컬 데이터베이스에 저장되는 미디어 이미지 정보 엔티티
 * 
 * @property id 이미지의 고유 ID
 * @property contentPath 이미지 파일 경로 (URI 문자열)
 * @property name 이미지 파일명
 * @property size 이미지 파일 크기 (바이트)
 * @property mimeType 이미지 MIME 타입 (예: image/jpeg)
 * @property dateAdded 이미지가 추가된 시간 (Unix timestamp)
 */
@Entity(tableName = "media_images")
data class MediaImageEntity(
    @PrimaryKey
    val id: String,
    val contentPath: String,
    val name: String = "",
    val size: Long = 0L,
    val mimeType: String = "",
    val dateAdded: Long = 0L
) 