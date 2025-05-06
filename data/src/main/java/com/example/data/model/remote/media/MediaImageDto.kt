package com.example.data.model.remote.media

/**
 * 미디어 이미지 원격 데이터 전송 객체
 * Firebase Storage에서 가져온 이미지 정보를 표현합니다.
 * 
 * @property id 이미지의 고유 ID (파일명 또는 생성된 UUID)
 * @property uri 이미지 다운로드 URL
 * @property name 이미지 파일명
 * @property path Firebase Storage 경로
 * @property mimeType 이미지 MIME 타입 (예: image/jpeg)
 * @property size 이미지 파일 크기 (바이트)
 * @property dateAdded 이미지가 추가된 시간 (Unix timestamp)
 */
data class MediaImageDto(
    val id: String,
    val uri: String,
    val name: String = "",
    val path: String = "",
    val mimeType: String = "",
    val size: Long = 0L,
    val dateAdded: Long = 0L
) 