package com.example.data.repository

import android.net.Uri
import com.example.core_common.error.DomainError
import com.example.core_common.error.ErrorMapper
import com.example.core_common.result.resultTry
import com.example.core_common.result.toDomainError
import com.example.data.datasource.remote.chat.ChatRemoteDataSource
import com.example.data.model.mapper.ChatMessageMapper
import com.example.data.model.mapper.MediaImageMapper
import com.example.domain.model.ChatMessage
import com.example.domain.model.MediaImage
import com.example.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result

/**
 * ChatRepository 인터페이스의 구현체
 * Firebase Firestore의 캐싱 기능을 활용하여 채팅 관련 기능을 구현합니다.
 * 
 * @property chatRemoteDataSource Firebase Firestore 채팅 데이터 소스
 * @property chatMessageMapper 채팅 메시지 데이터 매핑 유틸리티
 * @property mediaImageMapper 미디어 이미지 데이터 매핑 유틸리티
 * @property errorMapper 에러 매핑 유틸리티
 */
class ChatRepositoryImpl @Inject constructor(
    private val chatRemoteDataSource: ChatRemoteDataSource,
    private val chatMessageMapper: ChatMessageMapper,
    private val mediaImageMapper: MediaImageMapper,
    private val errorMapper: ErrorMapper
) : ChatRepository {

    /**
     * 특정 채널의 메시지 스트림을 가져옵니다.
     * Firestore에서 실시간 업데이트를 구독합니다.
     * 
     * @param channelId 채팅 채널 ID
     * @return 채팅 메시지 리스트를 포함하는 Flow
     */
    override fun getMessagesStream(channelId: String): Flow<List<ChatMessage>> {
        // Firebase의 실시간 업데이트를 구독하고 Domain 모델로 변환
        return chatRemoteDataSource.getMessagesStream(channelId)
            .map { dtoList -> 
                dtoList.map { chatMessageMapper.mapToDomain(it) }
            }
    }

    /**
     * 특정 메시지 ID 이전의 과거 메시지를 페이징 방식으로 가져옵니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param beforeMessageId 이 메시지 ID 이전의 메시지를 가져옴
     * @param limit 가져올 메시지 최대 개수
     * @return 과거 메시지 리스트를 포함한 Result
     */
    override suspend fun fetchPastMessages(channelId: String, beforeMessageId: Int, limit: Int): Result<List<ChatMessage>> {
        return resultTry {
            val messagesResult = chatRemoteDataSource.fetchPastMessages(channelId, beforeMessageId, limit)
            
            messagesResult.fold(
                onSuccess = { messageDtos ->
                    messageDtos.map { chatMessageMapper.mapToDomain(it) }
                },
                onFailure = { throw it }
            )
        }.toDomainError { error -> 
            errorMapper.mapToChatError(error, "과거 메시지를 가져오는 중 오류가 발생했습니다.")
        }
    }

    /**
     * 새 메시지를 전송합니다. 이미지 Uri 목록을 받아 처리합니다.
     * TODO: 실제 이미지 업로드 로직 구현 필요 (e.g., Firebase Storage)
     * 
     * @param channelId 채팅 채널 ID
     * @param message 메시지 내용
     * @param imageUris 첨부 이미지 Uri 목록
     * @return 전송된 메시지 정보를 포함한 Result
     */
    override suspend fun sendMessage(channelId: String, message: String, imageUris: List<Uri>): Result<ChatMessage> {
        // 메시지 내용 검증 (비어있는 경우)
        if (message.isBlank() && imageUris.isEmpty()) {
            return Result.failure(DomainError.ChatError.InvalidMessageContent("메시지 내용이나 첨부 파일 중 하나는 있어야 합니다."))
        }
        
        return resultTry {
            // TODO: 여기서 imageUris를 실제 스토리지에 업로드하고, 결과 URL 목록을 가져와야 함.
            // 현재는 임시로 Uri를 String 경로로 변환하여 RemoteDataSource에 전달.
            val attachmentPaths = uriListToPathList(imageUris)
            
            val result = chatRemoteDataSource.sendMessage(channelId, message, attachmentPaths) // Pass converted paths
            
            result.fold(
                onSuccess = { messageDto ->
                    chatMessageMapper.mapToDomain(messageDto)
                },
                onFailure = { throw it }
            )
        }.toDomainError { error ->
            errorMapper.mapToChatError(error, "메시지 전송에 실패했습니다.")
        }
    }

    /**
     * 기존 메시지를 수정합니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param chatId 수정할 메시지 ID
     * @param newMessage 새 메시지 내용
     * @return 작업 결과
     */
    override suspend fun editMessage(channelId: String, chatId: Int, newMessage: String): Result<Unit> {
        // 메시지 내용 검증 (비어있는 경우)
        if (newMessage.isBlank()) {
            return Result.failure(DomainError.ChatError.InvalidMessageContent("메시지 내용은 비어있을 수 없습니다."))
        }
        
        return resultTry {
            chatRemoteDataSource.editMessage(channelId, chatId, newMessage).getOrThrow()
        }.toDomainError { error ->
            errorMapper.mapToChatError(error, "메시지 수정에 실패했습니다.")
        }
    }

    /**
     * 메시지를 삭제합니다.
     * 
     * @param channelId 채팅 채널 ID
     * @param chatId 삭제할 메시지 ID
     * @return 작업 결과
     */
    override suspend fun deleteMessage(channelId: String, chatId: Int): Result<Unit> {
        return resultTry {
            chatRemoteDataSource.deleteMessage(channelId, chatId).getOrThrow()
        }.toDomainError { error ->
            errorMapper.mapToChatError(error, "메시지 삭제에 실패했습니다.")
        }
    }

    /**
     * 로컬 갤러리 이미지를 페이징 방식으로 가져옵니다.
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param pageSize 페이지당 이미지 수
     * @return 미디어 이미지 목록을 포함한 Result
     */
    override suspend fun getLocalGalleryImages(page: Int, pageSize: Int): Result<List<MediaImage>> {
        return resultTry {
            // 갤러리 접근 로직은 별도 구현 필요
            chatRemoteDataSource.getLocalGalleryImages(page, pageSize).map { 
                mediaImageMapper.mapToDomain(it) 
            }
        }.toDomainError { error ->
            errorMapper.mapToDataError(error, "갤러리 이미지를 가져오는 중 오류가 발생했습니다.")
        }
    }

    /**
     * Uri 리스트를 문자열 경로 리스트로 변환합니다.
     * @param uris 변환할 Uri 리스트
     * @return 문자열 경로 리스트
     */
    private fun uriListToPathList(uris: List<Uri>): List<String> {
        return uris.map { it.toString() }
    }
}