package com.example.data.datasource.local.media

import com.example.domain.model.MediaImage
import kotlin.Result

/**
 * 로컬 미디어 파일(예: 갤러리 이미지)에 접근하기 위한 데이터 소스 인터페이스입니다.
 */
interface LocalMediaDataSource {
    /**
     * 로컬 갤러리에서 이미지를 가져옵니다.
     *
     * @param page 페이지 번호 (페이징 사용 시). 1부터 시작합니다.
     * @param pageSize 페이지당 이미지 수.
     * @return 성공 시 갤러리 이미지 목록이 포함된 Result, 실패 시 에러 정보가 포함된 Result.
     */
    suspend fun getLocalGalleryImages(page: Int, pageSize: Int): Result<List<MediaImage>>
} 