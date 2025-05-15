package com.example.data.model.mapper

import android.net.Uri
import androidx.core.net.toUri
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.local.MediaImageEntity
import com.example.data.model.remote.media.MediaImageDto
import com.example.domain.model.MediaImage
import java.time.Instant
import javax.inject.Inject

/**
 * 미디어 이미지 모델 매핑을 위한 유틸리티 클래스
 * Domain 모델과 Entity, DTO 간의 변환을 담당합니다.
 */
class MediaImageMapper @Inject constructor(
    private val dateTimeUtil: DateTimeUtil
) {
    
    /**
     * Local Entity를 Domain 모델로 변환합니다.
     *
     * @param entity 변환할 MediaImageEntity 객체
     * @return 변환된 MediaImage 도메인 객체
     */
    fun mapToDomain(entity: MediaImageEntity): MediaImage {
        return MediaImage(
            id = entity.id,
            contentPath = entity.contentPath.toUri(),
            name = entity.name,
            size = entity.size,
            mimeType = entity.mimeType,
            dateAdded = dateTimeUtil.epochMilliToInstant(entity.dateAdded)
        )
    }
    
    /**
     * Remote DTO (MediaImageDto)를 Domain 모델 (MediaImage)로 변환합니다.
     * DTO의 'url' 필드를 Domain의 'contentPath' (Uri)로, 'fileName'을 'name'으로 변환합니다.
     *
     * @param dto 변환할 MediaImageDto 객체
     * @return 변환된 MediaImage 도메인 객체
     */
    fun mapToDomain(dto: MediaImageDto): MediaImage {
        return MediaImage(
            id = dto.id,
            contentPath = dto.url.toUri(),
            name = dto.fileName,
            size = dto.size,
            mimeType = dto.mimeType,
            dateAdded = dateTimeUtil.epochMilliToInstant(dto.dateAdded)
        )
    }
    
    /**
     * Domain 모델 (MediaImage)을 Local Entity (MediaImageEntity)로 변환합니다.
     *
     * @param domain 변환할 MediaImage 도메인 객체
     * @return 변환된 MediaImageEntity 객체
     */
    fun mapToEntity(domain: MediaImage): MediaImageEntity {
        return MediaImageEntity(
            id = domain.id,
            contentPath = domain.contentPath.toString(),
            name = domain.name,
            size = domain.size,
            mimeType = domain.mimeType,
            dateAdded = dateTimeUtil.instantToEpochMilli(domain.dateAdded)
        )
    }
    
    /**
     * Domain 모델 (MediaImage)을 Remote DTO (MediaImageDto)로 변환합니다.
     * Domain의 'contentPath' (Uri)를 DTO의 'url' (String)로, 'name'을 'fileName'으로 변환합니다.
     * DTO의 'type'은 "image"로, 'thumbnailUrl'은 null로 설정합니다.
     *
     * @param domain 변환할 MediaImage 도메인 객체
     * @return 변환된 MediaImageDto 객체
     */
    fun mapToDto(domain: MediaImage): MediaImageDto {
        return MediaImageDto(
            id = domain.id,
            url = domain.contentPath.toString(),
            fileName = domain.name,
            type = "image",
            path = "",
            mimeType = domain.mimeType,
            size = domain.size,
            thumbnailUrl = null,
            dateAdded = dateTimeUtil.instantToEpochMilli(domain.dateAdded)
        )
    }
    
    /**
     * Domain 모델 리스트를 Entity 리스트로 변환합니다.
     *
     * @param domainList 변환할 MediaImage 도메인 객체 리스트
     * @return 변환된 MediaImageEntity 객체 리스트
     */
    fun mapToEntityList(domainList: List<MediaImage>): List<MediaImageEntity> {
        return domainList.map { mapToEntity(it) }
    }
    
    /**
     * Entity 리스트를 Domain 모델 리스트로 변환합니다.
     *
     * @param entityList 변환할 MediaImageEntity 객체 리스트
     * @return 변환된 MediaImage 도메인 객체 리스트
     */
    fun mapEntityListToDomain(entityList: List<MediaImageEntity>): List<MediaImage> {
        return entityList.map { mapToDomain(it) }
    }
    
    /**
     * DTO 리스트를 Domain 모델 리스트로 변환합니다.
     *
     * @param dtoList 변환할 MediaImageDto 객체 리스트
     * @return 변환된 MediaImage 도메인 객체 리스트
     */
    fun mapDtoListToDomain(dtoList: List<MediaImageDto>): List<MediaImage> {
        return dtoList.map { mapToDomain(it) }
    }
} 