package com.example.feature_chat.service

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import com.example.domain_repository.local.LocalMessagePagingRepository
import com.example.domain_usecase.provider.chat.ChatUseCases
import com.example.feature_chat.queue.OfflineMessageQueue
import com.example.websocket.usecase.WebSocketUseCaseProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 메시지 관리 서비스 - Paging3 + UseCase 패턴 기반
 *
 * 핵심 변경사항:
 * - memoryManager 제거 → Room DB가 Single Source of Truth
 * - WebSocket → core에서 자동으로 Room DB 저장
 * - UI → Room DB에서 Paging3로 읽기
 * - UseCase 패턴으로 Clean Architecture 준수
 */
class MessageService @Inject constructor(
    private val chatUseCases: ChatUseCases,
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    private val offlineMessageQueue: OfflineMessageQueue,
    private val localMessageRepository: LocalMessagePagingRepository,
    private val roomId: String,
    private val projectId: String? = null,
    private val channelType: String = "chat"
) {

    companion object {
        private const val TAG = "MessageService"
        private const val PAGE_SIZE = 50
        private const val PREFETCH_DISTANCE = 10
    }

    // WebSocket 사용 사례 (방별)
    private val roomWebSocketUseCases = webSocketUseCaseProvider.createForRoom(roomId)

    // Paging3 설정
    private val messagesPager = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = {
            localMessageRepository.getMessagesPagingSource()
        }
    )

    // ================================
    // 메시지 읽기 (Paging3)
    // ================================
    
    /**
     * Room DB에서 Paging3를 통한 메시지 스트림
     * WebSocket으로 받은 메시지들이 자동으로 포함됨
     */
    fun getMessagesPagingFlow(): Flow<PagingData<Message>> {
        Log.d(TAG, "메시지 Paging Flow 제공 - Room DB 기반")
        return messagesPager.flow
    }
    
    /**
     * 초기 메시지 로딩 (Firestore → Room DB 동기화)
     */
    suspend fun loadInitialMessages(): CustomResult<Unit, Exception> {
        Log.d(TAG, "초기 메시지 로딩 시작 - Firestore → Room DB 동기화")

        return try {
            when (val result = chatUseCases.fetchPastMessagesUseCase(limit = PAGE_SIZE)) {
                is CustomResult.Success -> {
                    Log.d(TAG, "Firestore에서 ${result.data.size}개 메시지 가져옴")

                    // Room DB에 저장 (중복 방지는 Repository에서 처리)
                    result.data.forEach { message ->
                        localMessageRepository.save(message)
                    }

                    Log.d(TAG, "초기 메시지 Room DB 저장 완료")
                    CustomResult.Success(Unit)
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "초기 메시지 로딩 실패", result.error)
                    CustomResult.Failure(result.error)
                }

                else -> {
                    Log.d(TAG, "초기 메시지 로딩 진행 중...")
                    CustomResult.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "초기 메시지 로딩 중 예외", e)
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 메시지 전송 (WebSocket + UseCase)
    // ================================
    
    /**
     * 메시지 전송 - WebSocket UseCase 사용
     */
    suspend fun sendMessage(
        senderId: UserId,
        content: String,
        replyToMessageId: DocumentId? = null
    ): CustomResult<DocumentId, Exception> {
        Log.d(TAG, "메시지 전송 시도: senderId=${senderId.value}, roomId=$roomId")

        return try {
            // 고유 메시지 ID 생성
            val messageId = DocumentId.generate()

            // WebSocket을 통한 메시지 전송
            val sendResult = roomWebSocketUseCases.sendMessageUseCase(
                senderId = senderId,
                content = content,
                messageId = messageId,
                replyToMessageId = replyToMessageId,
                projectId = projectId,
                channelType = channelType
            )

            if (sendResult.isSuccess) {
                Log.d(TAG, "메시지 전송 성공: ${messageId.value}")

                // 임시 메시지를 Room DB에 저장 (상태: 전송 중)
                val tempMessage = Message.create(
                    id = messageId,
                    senderId = senderId,
                    content = MessageContent(content),
                    replyToMessageId = replyToMessageId,
                    mentions = emptyList()
                )

                localMessageRepository.save(tempMessage)

                CustomResult.Success(messageId)
            } else {
                Log.e(TAG, "메시지 전송 실패: ${sendResult.exceptionOrNull()?.message}")

                // 오프라인 큐에 추가 (간단한 큐 구조 사용)
                val tempMessage = Message.create(
                    id = messageId,
                    senderId = senderId,
                    content = MessageContent(content),
                    replyToMessageId = replyToMessageId,
                    mentions = emptyList()
                )
                offlineMessageQueue.queueMessage(
                    com.example.feature_chat.queue.QueuedMessageAction.Send(
                        message = tempMessage,
                        roomId = roomId
                    )
                )

                CustomResult.Failure(Exception(sendResult.exceptionOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "메시지 전송 중 예외", e)
            CustomResult.Failure(e)
        }
    }
    
    /**
     * 메시지 수정 - WebSocket UseCase 사용
     */
    suspend fun editMessage(
        messageId: DocumentId,
        newContent: String
    ): CustomResult<Unit, Exception> {
        Log.d(TAG, "메시지 수정 시도: messageId=${messageId.value}")

        return try {
            val editResult = roomWebSocketUseCases.editMessageUseCase(
                messageId = messageId,
                newContent = newContent,
                projectId = projectId,
                channelType = channelType
            )

            if (editResult.isSuccess) {
                Log.d(TAG, "메시지 수정 성공: ${messageId.value}")

                // Room DB에서 메시지 업데이트 (WebSocket 이벤트로도 업데이트되지만 즉시 반영용)
                val existingMessage = localMessageRepository.findById(messageId)
                if (existingMessage is CustomResult.Success) {
                    existingMessage.data.updateContent(MessageContent(newContent))
                    localMessageRepository.save(existingMessage.data)
                }

                CustomResult.Success(Unit)
            } else {
                Log.e(TAG, "메시지 수정 실패: ${editResult.exceptionOrNull()?.message}")
                CustomResult.Failure(Exception(editResult.exceptionOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "메시지 수정 중 예외", e)
            CustomResult.Failure(e)
        }
    }
    
    /**
     * 메시지 삭제 - WebSocket UseCase 사용
     */
    suspend fun deleteMessage(messageId: DocumentId): CustomResult<Unit, Exception> {
        Log.d(TAG, "메시지 삭제 시도: messageId=${messageId.value}")

        return try {
            val deleteResult = roomWebSocketUseCases.deleteMessageUseCase(
                messageId = messageId,
                projectId = projectId,
                channelType = channelType
            )

            if (deleteResult.isSuccess) {
                Log.d(TAG, "메시지 삭제 성공: ${messageId.value}")

                // Room DB에서 메시지 삭제 마킹 (WebSocket 이벤트로도 처리되지만 즉시 반영용)
                val existingMessage = localMessageRepository.findById(messageId)
                if (existingMessage is CustomResult.Success) {
                    existingMessage.data.delete()
                    localMessageRepository.save(existingMessage.data)
                }

                CustomResult.Success(Unit)
            } else {
                Log.e(TAG, "메시지 삭제 실패: ${deleteResult.exceptionOrNull()?.message}")
                CustomResult.Failure(Exception(deleteResult.exceptionOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "메시지 삭제 중 예외", e)
            CustomResult.Failure(e)
        }
    }

    // ================================
    // WebSocket 상태 관리
    // ================================
    
    /**
     * WebSocket 연결 상태 확인
     */
    fun getConnectionState() = webSocketUseCaseProvider.create().getConnectionStateUseCase()
    
    /**
     * WebSocket 인증 상태 확인  
     */
    fun getAuthenticationState() = webSocketUseCaseProvider.create().getAuthenticationStateUseCase()
    
    /**
     * 방 입장 상태 확인
     */
    fun isRoomJoined(): Boolean = roomWebSocketUseCases.isRoomJoinedUseCase()
    
    /**
     * 방 입장
     */
    suspend fun joinRoom(userId: UserId? = null): CustomResult<Unit, Exception> {
        Log.d(TAG, "방 입장 시도: roomId=$roomId")
        
        return try {
            val joinResult = roomWebSocketUseCases.joinRoomUseCase(userId)

            if (joinResult.isSuccess) {
                Log.d(TAG, "방 입장 성공: $roomId")
                CustomResult.Success(Unit)
            } else {
                Log.e(TAG, "방 입장 실패: ${joinResult.exceptionOrNull()?.message}")
                CustomResult.Failure(Exception(joinResult.exceptionOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "방 입장 중 예외", e)
            CustomResult.Failure(e)
        }
    }

    /**
     * 방 퇴장
     */
    suspend fun leaveRoom(): CustomResult<Unit, Exception> {
        Log.d(TAG, "방 퇴장 시도: roomId=$roomId")
        
        return try {
            val leaveResult = roomWebSocketUseCases.leaveRoomUseCase()

            if (leaveResult.isSuccess) {
                Log.d(TAG, "방 퇴장 성공: $roomId")
                CustomResult.Success(Unit)
            } else {
                Log.e(TAG, "방 퇴장 실패: ${leaveResult.exceptionOrNull()?.message}")
                CustomResult.Failure(Exception(leaveResult.exceptionOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "방 퇴장 중 예외", e)
            CustomResult.Failure(e)
        }
    }

    // ================================
    // WebSocket 이벤트 구독
    // ================================

    /**
     * 이 방의 WebSocket 이벤트 구독
     */
    fun subscribeToRoomEvents() = roomWebSocketUseCases.subscribeToRoomEventsUseCase()

    /**
     * 이 방의 메시지 이벤트만 구독
     */
    fun subscribeToMessageEvents() = roomWebSocketUseCases.subscribeToRoomMessageEventsUseCase()

    /**
     * 특정 메시지의 ACK 이벤트 구독
     */
    fun subscribeToMessageAck(messageId: String) =
        roomWebSocketUseCases.subscribeToMessageAckEventsUseCase(messageId)
}