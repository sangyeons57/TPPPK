package com.example.domain.usecase.chat

import android.net.Uri
import com.example.domain.model.ChatMessage
import com.example.domain.repository.ChatRepository
import javax.inject.Inject
import kotlin.Result

/**
 * 채팅 메시지를 전송하는 UseCase
 * 
 * @property chatRepository 채팅 관련 기능을 제공하는 Repository
 */
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * 텍스트 및 이미지가 포함된 메시지를 전송합니다.
     *
     * @param channelId 채팅 채널 ID
     * @param message 전송할 텍스트 메시지
     * @param imageUris 첨부할 이미지 URI 목록
     * @return 성공 시 전송된 메시지가 포함된 Result, 실패 시 에러 정보가 포함된 Result
     */
    suspend operator fun invoke(channelId: String, message: String, imageUris: List<Uri> = emptyList()): Result<ChatMessage> {
        // 입력 유효성 검사
        if (channelId.isBlank()) {
            return Result.failure(IllegalArgumentException("채널 ID는 비어있을 수 없습니다."))
        }

        // 메시지와 이미지가 모두 비어있는 경우 검사
        if (message.isBlank() && imageUris.isEmpty()) {
            return Result.failure(IllegalArgumentException("메시지 내용이나 이미지 중 하나는 있어야 합니다."))
        }

        // Uri 유효성 검사는 Repository에서 처리하거나 필요시 여기서 추가

        // 메시지 내용 정리 (앞뒤 공백 제거)
        val trimmedMessage = message.trim()

        // Repository 호출하여 메시지 전송
        return chatRepository.sendMessage(channelId, trimmedMessage, imageUris)
    }
} 