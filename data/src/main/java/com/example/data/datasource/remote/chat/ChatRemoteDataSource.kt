package com.example.data.datasource.remote.chat

import com.example.data.model.remote.chat.ChatMessageDto
import kotlinx.coroutines.flow.Flow

/**
 * 채팅 메시지 관련 원격 데이터 소스 인터페이스입니다.
 * Firestore 또는 다른 채팅 백엔드와의 통신을 담당합니다.
 */
interface ChatRemoteDataSource {
    // 특정 채널의 메시지 목록 가져오기 (페이징 처리 포함)
    suspend fun getMessages(channelId: String, beforeMessageId: Int?, limit: Int): List<ChatMessageDto>

    // 새 메시지 전송
    suspend fun sendMessage(channelId: String, messageDto: ChatMessageDto): ChatMessageDto // 전송 결과 반환

    // 메시지 수정
    suspend fun editMessage(channelId: String, messageId: Int, newContent: String)

    // 메시지 삭제
    suspend fun deleteMessage(channelId: String, messageId: Int)

    // ... 향후 필요한 채팅 관련 원격 데이터 처리 함수 추가 ...
} 