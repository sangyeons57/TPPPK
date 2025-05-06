package com.example.domain.repository

import android.net.Uri
import com.example.domain.model.ChatMessage
import com.example.domain.model.MediaImage
import kotlinx.coroutines.flow.Flow

// --- Repository 인터페이스 (가상) ---
/**
 * 채팅 관련 데이터 처리를 위한 Repository 인터페이스입니다.
 */
interface ChatRepository {
    // 특정 채널의 메시지 목록을 가져오는 함수 (페이징 지원)
    // Flow를 사용하면 실시간 업데이트 구현 가능
    fun getMessagesStream(channelId: String): Flow<List<ChatMessage>>

    // 특정 시점 이전의 메시지를 더 가져오는 함수 (페이징)
    suspend fun fetchPastMessages(channelId: String, beforeMessageId: Int, limit: Int): Result<List<ChatMessage>>

    // 메시지 전송
    /**
     * 지정된 채널에 메시지를 전송합니다. 이미지 첨부 파일을 포함할 수 있습니다.
     * @param channelId 메시지를 보낼 채널 ID
     * @param message 보낼 메시지 텍스트
     * @param imageUris 첨부할 이미지 파일의 Uri 목록
     * @return 성공 시 전송된 메시지 정보, 실패 시 에러를 포함한 Result
     */
    suspend fun sendMessage(
        channelId: String,
        message: String,
        imageUris: List<Uri> // 이미지 Uri 목록
    ): Result<ChatMessage> // 성공 시 전송된 메시지 정보 반환

    // 메시지 수정
    suspend fun editMessage(channelId: String, chatId: Int, newMessage: String): Result<Unit>

    // 메시지 삭제
    suspend fun deleteMessage(channelId: String, chatId: Int): Result<Unit>

    // 로컬 갤러리 이미지 가져오기 (페이징 지원)
    suspend fun getLocalGalleryImages(page: Int, pageSize: Int): Result<List<MediaImage>>
}

