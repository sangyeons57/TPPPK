package com.example.data.model.mapper

import com.example.data.model.remote.project.ChannelDto
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.google.firebase.Timestamp
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChannelDto와 Channel 도메인 모델 간의 변환을 담당하는 매퍼 클래스
 */
@Singleton
class ChannelMapper @Inject constructor() {

    /**
     * ChannelDto를 Channel 도메인 모델로 변환합니다.
     *
     * @param dto 변환할 ChannelDto 객체
     * @param categoryId 채널이 속한 카테고리 ID
     * @param projectId 채널이 속한 프로젝트 ID
     * @return 변환된 Channel 도메인 모델
     */
    fun mapToDomain(dto: ChannelDto, categoryId: String, projectId: String): Channel {
        return Channel(
            id = dto.channelId,
            categoryId = categoryId,
            projectId = projectId,
            name = dto.name,
            type = parseChannelType(dto.type),
            order = dto.order
        )
    }

    /**
     * Channel 도메인 모델을 ChannelDto로 변환합니다.
     *
     * @param domainModel 변환할 Channel 도메인 모델
     * @param userId 현재 로그인한 사용자 ID (새 채널 생성 시 createdBy 필드에 사용)
     * @return 변환된 ChannelDto 객체
     */
    fun mapToDto(domainModel: Channel, userId: String): ChannelDto {
        return ChannelDto(
            channelId = domainModel.id,
            name = domainModel.name,
            type = domainModel.type.name,
            order = domainModel.order,
            createdAt = Timestamp.now(),
            createdBy = userId
        )
    }

    /**
     * ChannelDto 목록을 Channel 도메인 모델 목록으로 변환합니다.
     *
     * @param dtoList 변환할 ChannelDto 목록
     * @param categoryId 채널이 속한 카테고리 ID
     * @param projectId 채널이 속한 프로젝트 ID
     * @return 변환된 Channel 도메인 모델 목록
     */
    fun mapToDomainList(dtoList: List<ChannelDto>, categoryId: String, projectId: String): List<Channel> {
        return dtoList.map { mapToDomain(it, categoryId, projectId) }
    }

    /**
     * 문자열 채널 타입을 ChannelType enum으로 변환합니다.
     *
     * @param typeString 변환할 채널 타입 문자열
     * @return 변환된 ChannelType 열거형 값
     */
    private fun parseChannelType(typeString: String): ChannelType {
        return when (typeString.uppercase()) {
            "VOICE" -> ChannelType.VOICE
            else -> ChannelType.TEXT
        }
    }
} 