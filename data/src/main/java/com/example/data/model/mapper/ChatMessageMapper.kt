package com.example.data.model.mapper

import com.example.data.model.local.chat.ChatMessageEntity
import com.example.data.model.remote.chat.ChatMessageDto
import com.example.domain.model.ChatMessage
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * 채팅 메시지 모델 매핑을 위한 유틸리티 클래스
 * Domain, DTO, Entity 간의 변환을 담당합니다.
 */
class ChatMessageMapper @Inject constructor() {
    
    /**
     * Remote DTO를 Domain 모델로 변환합니다.
     *
     * @param dto 변환할 ChatMessageDto 객체
     * @return 변환된 ChatMessage 도메인 객체
     */
    fun mapToDomain(dto: ChatMessageDto): ChatMessage {
        return ChatMessage(
            chatId = dto.chatId,
            channelId = dto.channelId,
            userId = dto.userId,
            userName = dto.userName,
            userProfileUrl = dto.userProfileUrl,
            message = dto.message,
            sentAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(dto.sentAt),
                ZoneId.systemDefault()
            ),
            isModified = dto.isModified,
            attachmentImageUrls = dto.attachmentImageUrls ?: emptyList()
        )
    }
    
    /**
     * Domain 모델을 Remote DTO로 변환합니다.
     *
     * @param domain 변환할 ChatMessage 도메인 객체
     * @return 변환된 ChatMessageDto 객체
     */
    fun mapToDto(domain: ChatMessage): ChatMessageDto {
        return ChatMessageDto(
            chatId = domain.chatId,
            channelId = domain.channelId,
            userId = domain.userId,
            userName = domain.userName,
            userProfileUrl = domain.userProfileUrl,
            message = domain.message,
            sentAt = domain.sentAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            isModified = domain.isModified,
            attachmentImageUrls = domain.attachmentImageUrls
        )
    }
    
    /**
     * Local Entity를 Remote DTO로 변환합니다.
     *
     * @param entity 변환할 ChatMessageEntity 객체
     * @return 변환된 ChatMessageDto 객체
     */
    fun mapEntityToDto(entity: ChatMessageEntity): ChatMessageDto {
        return ChatMessageDto(
            chatId = entity.chatId,
            channelId = entity.channelId,
            userId = entity.userId,
            userName = entity.userName,
            userProfileUrl = entity.userProfileUrl,
            message = entity.message,
            sentAt = entity.sentAt,
            isModified = entity.isModified,
            attachmentImageUrls = entity.attachmentImageUrls?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }
    
    /**
     * Remote DTO를 Local Entity로 변환합니다.
     *
     * @param dto 변환할 ChatMessageDto 객체
     * @return 변환된 ChatMessageEntity 객체
     */
    fun mapDtoToEntity(dto: ChatMessageDto): ChatMessageEntity {
        return ChatMessageEntity(
            chatId = dto.chatId,
            channelId = dto.channelId,
            userId = dto.userId,
            userName = dto.userName,
            userProfileUrl = dto.userProfileUrl,
            message = dto.message,
            sentAt = dto.sentAt,
            isModified = dto.isModified,
            attachmentImageUrls = dto.attachmentImageUrls?.joinToString(",") ?: ""
        )
    }
} 