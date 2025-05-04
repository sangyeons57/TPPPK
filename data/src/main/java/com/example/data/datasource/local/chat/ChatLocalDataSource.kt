package com.example.data.datasource.local.chat

import com.example.data.model.local.ChatEntity
import com.example.data.model.local.chat.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * 채팅 메시지 데이터의 로컬 데이터 소스 인터페이스입니다.
 * 주로 Room 데이터베이스와 상호작용합니다.
 */
interface ChatLocalDataSource {
    // 특정 채널의 메시지 스트림 가져오기
    fun getMessagesStream(channelId: String): Flow<List<ChatMessageEntity>>

    // 특정 채널의 메시지 목록 저장 (페이징 고려)
    suspend fun saveMessages(channelId: String, messages: List<ChatMessageEntity>)

    // 단일 메시지 추가 또는 업데이트 (Upsert)
    suspend fun upsertMessage(message: ChatMessageEntity)

    // 메시지 삭제
    suspend fun deleteMessage(messageId: String)

    // 특정 채널의 모든 메시지 삭제
    suspend fun clearMessagesForChannel(channelId: String)

    // ... 향후 필요한 채팅 관련 로컬 데이터 처리 함수 추가 ...
} 