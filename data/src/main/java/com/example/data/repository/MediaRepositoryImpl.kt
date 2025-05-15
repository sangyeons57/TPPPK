package com.example.data.repository

import com.example.data.datasource.local.media.LocalMediaDataSource
import com.example.domain.model.MediaImage
import com.example.domain.repository.MediaRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result

/**
 * MediaRepository의 구현체입니다.
 * LocalMediaDataSource를 통해 로컬 미디어 데이터를 가져옵니다.
 */
@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val localMediaDataSource: LocalMediaDataSource
) : MediaRepository {

    /**
     * 로컬 갤러리에서 이미지를 가져옵니다.
     * 작업은 LocalMediaDataSource에 위임됩니다.
     */
    override suspend fun getLocalGalleryImages(page: Int, pageSize: Int): Result<List<MediaImage>> {
        return localMediaDataSource.getLocalGalleryImages(page, pageSize)
    }
} 