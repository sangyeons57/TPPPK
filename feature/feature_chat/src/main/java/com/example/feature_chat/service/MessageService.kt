package com.example.feature_chat.service

import android.net.Uri
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.core_common.result.CustomResult
import com.example.core_common.util.AuthUtil
import com.example.core_common.util.DateTimeUtil
import com.example.domain.enum.OutBoxStatus
import com.example.domain.model.base.Message
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.domain.vo.user.UserName
import com.example.domain_repository.base.MessageRepository
import com.example.domain_usecase.provider.dm.DMUseCaseProvider
import com.example.domain_usecase.provider.file.FileManagementUseCases
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.MessageDeliveryState
import com.example.feature_chat.queue.OfflineMessageQueue
import com.example.websocket.usecase.WebSocketUseCaseProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * 메시지 관리 서비스 - 간단한 페이징 기능
 *
 * 역할:
 * - 메시지 전송/수정/삭제 (WebSocket UseCase 활용)
 * - Paging3 플로우 제공 (Room DB 기반)
 * - UI 모델 변환 (Message → ChatMessageUiModel)
 * - 오프라인 메시지 큐잉
 */
class MessageService @Inject constructor(
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    private val offlineMessageQueue: OfflineMessageQueue,
    private val messageRepository: MessageRepository,
    private val userProfileService: UserProfileService,
    private val fileUseCases: FileManagementUseCases,
    private val dmUseCaseProvider: DMUseCaseProvider,
    private val roomId: String,
    private val projectId: String? = null,
    private val channelType: String = "chat"
) {

    companion object {
        private const val TAG = "MessageService"
        private const val PAGE_SIZE = 15 // 15개씩 로딩
        private const val INITIAL_LOAD_SIZE = PAGE_SIZE * 2 // 초기 로드는 2페이지 정도만 로드 (뷰포트 채움 최소화)
        private const val MAX_SIZE = PAGE_SIZE * 3 // 메모리 내 유지 아이템 수 제한
        private const val PREFETCH_DISTANCE = 1 // 경계에 근접 시 다음 페이지 로드 (0은 경계 인식 문제로 비권장)
    }

    // OutBox 상태 캐시 (메시지 ID -> OutBox 상태)
    private val outboxStatusCache = mutableMapOf<String, OutBoxStatus>()

    /**
     * OutBox 상태를 캐시에 업데이트
     */
    fun updateOutBoxStatusCache(messageId: String, status: OutBoxStatus) {
        outboxStatusCache[messageId] = status
        Log.d(TAG, "OutBox 상태 캐시 업데이트: $messageId -> $status")
    }

    /**
     * 캐시된 OutBox 상태 조회 (non-suspend)
     */
    private fun getCachedOutBoxStatus(messageId: String): OutBoxStatus? {
        return outboxStatusCache[messageId]
    }

    // WebSocket 사용 사례 (방별) - 메시지 전송용으로만 사용
    private val roomWebSocketUseCases = webSocketUseCaseProvider.createForRoom(roomId)

    // Paging3 설정 - 기본 페이지 (최신 메시지부터)
    private val messagesPager = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = INITIAL_LOAD_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            maxSize = MAX_SIZE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = {
            messageRepository.getMessagesPagingSource(roomId)
        }
    )

    // ================================
    // 메시지 읽기 (Paging3) - UI 모델 변환 포함
    // ================================
    
    /**
     * Room DB에서 Paging3를 통한 메시지 스트림 (도메인 모델)
     * WebSocket으로 받은 메시지들이 자동으로 포함됨
     */
    fun getMessagesPagingFlow(): Flow<PagingData<Message>> {
        Log.d(TAG, "메시지 Paging Flow 제공 - Room DB 기반")
        return messagesPager.flow
    }

    /**
     * UI에서 직접 사용할 수 있도록 UI 모델로 변환된 Paging3 스트림 제공
     * ViewModel에서 runBlocking을 사용하지 않도록 여기서 변환을 수행
     */
    fun getUiMessagesPagingFlow(): Flow<PagingData<ChatMessageUiModel>> {
        Log.d(TAG, "UI 메시지 Paging Flow 제공 - UI 모델 변환 포함")
        return messagesPager.flow.map { pagingData ->
            pagingData.map { message -> convertDomainMessageToUiModel(message) }
        }
    }

    /**
     * 도메인 Message를 ChatMessageUiModel로 변환
     * 캐싱을 통한 성능 최적화
     */
    fun convertDomainMessageToUiModel(message: Message): ChatMessageUiModel {
        // 채널 혼입 진단: 다른 채널 데이터가 섞여오면 즉시 경고
        if (message.channelId.value != roomId) {
            Log.w(
                TAG,
                "채널 불일치 감지: expected=$roomId, actual=${message.channelId.value}, messageId=${message.id.value}, createdAt=${message.createdAt}"
            )
        }
        val currentUserId = AuthUtil.getCurrentUserId()
        val senderId = message.senderId.value

        // 사용자 프로필 정보는 캐시에서 우선 조회, 없으면 동기 로딩 시도(최대 150ms)
        if (!userProfileService.isUserCached(senderId)) {
            try {
                runBlocking {
                    // 짧은 시간만 대기하여 UI 스레드 블로킹을 최소화
                    loadWithTimeout(senderId)
                }
            } catch (_: Exception) { /* ignore */
            }
        }

        // OutBox 상태 확인 - 캐시에서 조회 (non-suspend)
        val outboxStatus = getCachedOutBoxStatus(message.id.value)

        // OutBox 상태에 따른 전송 상태 판단
        val isSending = outboxStatus == OutBoxStatus.PENDING
        val sendFailed = outboxStatus == OutBoxStatus.FAILED
        val isDispatched = outboxStatus == OutBoxStatus.DISPATCHED

        // 디버그 로그 추가
        Log.d(
            TAG,
            "메시지 상태 확인: messageId=${message.id.value}, outboxStatus=$outboxStatus, isSending=$isSending, sendFailed=$sendFailed, isDispatched=$isDispatched"
        )

        return ChatMessageUiModel(
            messageId = message.id.value,
            userId = senderId,
            userName = userProfileService.getUserDisplayName(senderId),
            userProfileUrl = userProfileService.getCachedProfileUrl(senderId),
            messageType = message.messageType,
            message = message.payload.getTextContent() ?: "",
            payload = message.payload.toString(),
            formattedTimestamp = DateTimeUtil.formatChatTime(message.createdAt),
            actualTimestamp = message.createdAt,
            isModified = false, // TODO: 수정 여부 확인
            attachmentImageUrls = emptyList(), // TODO: 첨부파일 처리
            isMyMessage = senderId == currentUserId,
            isSending = isSending,
            sendFailed = sendFailed,
            isDeleted = message.isDeleted.value,
            deliveryState = when {
                isSending -> MessageDeliveryState.Sending
                sendFailed -> MessageDeliveryState.Failed("메시지 전송에 실패했습니다")
                isDispatched -> MessageDeliveryState.Sent
                else -> MessageDeliveryState.Sent // 기본값은 전송 완료로 간주
            },
            isOptimistic = isSending,
            clientSentAt = null,
            retryCount = 0,
            canRetry = sendFailed, // 전송 실패한 경우에만 재전송 가능
            errorMessage = if (sendFailed) "메시지 전송에 실패했습니다" else null,
            replyToMessageId = message.replyToMessageId?.value,
            replyToContent = null, // TODO: 답장 대상 메시지 내용 조회
            replyToUserName = null, // TODO: 답장 대상 사용자 이름 조회
            mentions = message.mentions,
            isMentionedMessage = false // TODO: 현재 사용자 멘션 여부 확인
        )
    }

    private suspend fun loadWithTimeout(userId: String) {
        // 150ms 내에 프로필 불러오기 시도 (캐시 미스 시만)
        try {
            kotlinx.coroutines.withTimeout(400) {
                userProfileService.loadUserProfile(userId)
            }
        } catch (_: Exception) { /* timeout or error - fallback to unknown */
        }
    }

    // ================================
    // Anchor Jump 기능
    // ================================

    private var latestMessageId: String? = null
    private val _isAnchorJumpInProgress = MutableStateFlow(false)
    val isAnchorJumpInProgress: StateFlow<Boolean> = _isAnchorJumpInProgress.asStateFlow()

    // 현재 점프 대상 메시지 ID (UI가 인덱스 계산에 사용)
    private val _anchorTargetMessageId = MutableStateFlow<String?>(null)
    val anchorTargetMessageId: StateFlow<String?> = _anchorTargetMessageId.asStateFlow()

    /**
     * 초기 Anchor 설정
     */
    suspend fun setupInitialAnchor(initialMessageId: String?) {
        Log.d(TAG, "초기 Anchor 설정: $initialMessageId")
        _isAnchorJumpInProgress.value = true

        try {
            if (initialMessageId != null) {
                _anchorTargetMessageId.value = initialMessageId
                jumpToMessage(initialMessageId)
            } else {
                jumpToLatest()
            }
        } finally {
            _isAnchorJumpInProgress.value = false
        }
    }

    /**
     * 특정 메시지로 점프
     */
    suspend fun jumpToMessage(messageId: String) {
        Log.d(TAG, "특정 메시지로 점프: $messageId")
        _isAnchorJumpInProgress.value = true
        _anchorTargetMessageId.value = messageId

        try {
            // 1) 메시지 타임스탬프 조회
            val anchorResult = messageRepository.findById(DocumentId(messageId))
            val anchorTsMs = if (anchorResult is CustomResult.Success) {
                anchorResult.data.createdAt.toEpochMilli()
            } else {
                // 메시지가 로컬에 없으면 현재 시간 기준으로만 프리페치 시도
                System.currentTimeMillis()
            }

            // 2) 앵커 주변 범위 프리페치 (로컬 캐시 기준)
            runCatching {
                messageRepository.getMessagesBefore(roomId, anchorTsMs, PAGE_SIZE)
            }
            runCatching {
                messageRepository.getMessagesAfter(roomId, anchorTsMs, PAGE_SIZE)
            }

            Log.d(TAG, "메시지 점프 주변 프리페치 완료: $messageId @ $anchorTsMs")
        } finally {
            _isAnchorJumpInProgress.value = false
        }
    }

    /**
     * 최신 메시지로 점프 (최신 메시지 ID 기억 방식)
     */
    suspend fun jumpToLatest() {
        Log.d(TAG, "최신 메시지로 점프")
        _isAnchorJumpInProgress.value = true

        try {
            // 최신 메시지 ID가 있으면 해당 메시지로 점프
            if (latestMessageId != null) {
                jumpToMessage(latestMessageId!!)
            } else {
                // 최신 메시지 ID가 없으면 단순히 로그만 출력
                Log.d(TAG, "최신 메시지 ID가 없음 - 기본 동작")
            }
        } finally {
            _isAnchorJumpInProgress.value = false
        }
    }

    /**
     * 최신 메시지 ID 업데이트 (새 메시지 수신 시 호출)
     */
    fun updateLatestMessageId(messageId: String) {
        latestMessageId = messageId
        Log.d(TAG, "최신 메시지 ID 업데이트: $messageId")
    }

    // ================================
    // 메시지 전송 (WebSocket + UseCase)
    // ================================
    
    /**
     * 메시지 전송 - WebSocket UseCase 사용
     */
    suspend fun sendMessage(
        senderId: UserId,
        messageType: MessageType = MessageType.TEXT,
        payload: MessagePayload,
        replyToMessageId: DocumentId? = null
    ): CustomResult<DocumentId, Exception> {
        Log.d(
            TAG,
            "메시지 전송 시도: senderId=${senderId.value}, type=${messageType.name}, roomId=$roomId"
        )
        return sendMessageInternal(senderId, messageType, payload, replyToMessageId)
    }

    /**
     * 텍스트 메시지 전송 (편의 메서드)
     */
    suspend fun sendTextMessage(
        senderId: UserId,
        content: String,
        replyToMessageId: DocumentId? = null
    ): CustomResult<DocumentId, Exception> {
        Log.d(TAG, "텍스트 메시지 전송 시도: senderId=${senderId.value}, content=$content, roomId=$roomId")
        val payload = MessagePayload.forText(content)
        return sendMessageInternal(senderId, MessageType.TEXT, payload, replyToMessageId)
    }

    /**
     * 이미지 메시지 전송 (단일 이미지)
     */
    suspend fun sendImageMessage(
        senderId: UserId,
        imageUri: Uri,
        content: String = "",
        replyToMessageId: DocumentId? = null
    ): CustomResult<DocumentId, Exception> {
        Log.d(TAG, "이미지 메시지 전송 시도: senderId=${senderId.value}, imageUri=$imageUri, roomId=$roomId")

        return try {
            // 이미지 업로드
            val uploadPath = "chat_images/${roomId}/${System.currentTimeMillis()}.jpg"
            val uploadResult = fileUseCases.uploadMediaUseCase(imageUri, uploadPath)

            when (uploadResult) {
                is CustomResult.Success -> {
                    val imageUrl = uploadResult.data
                    Log.d(TAG, "✅ 이미지 업로드 성공: $imageUrl")

                    // 이미지 메시지 페이로드 생성
                    val payload = MessagePayload.forImage(
                        content = content,
                        imageUrl = imageUrl,
                        imageFilename = imageUri.lastPathSegment
                    )

                    sendMessageInternal(senderId, MessageType.TEXT, payload, replyToMessageId)
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "❌ 이미지 업로드 실패", uploadResult.error)
                    CustomResult.Failure(uploadResult.error)
                }

                else -> {
                    Log.e(TAG, "❌ 이미지 업로드 알 수 없는 상태: $uploadResult")
                    CustomResult.Failure(Exception("이미지 업로드 실패"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "이미지 메시지 전송 중 예외", e)
            CustomResult.Failure(e)
        }
    }

    /**
     * 이미지 메시지 전송 (다중 이미지)
     */
    suspend fun sendImagesMessage(
        senderId: UserId,
        imageUris: List<Uri>,
        content: String = "",
        replyToMessageId: DocumentId? = null
    ): CustomResult<DocumentId, Exception> {
        Log.d(
            TAG,
            "이미지들 메시지 전송 시도: senderId=${senderId.value}, images=${imageUris.size}개, roomId=$roomId"
        )

        return try {
            val uploadedImages = mutableListOf<Map<String, Any?>>()

            // 각 이미지를 순차적으로 업로드
            for ((index, imageUri) in imageUris.withIndex()) {
                val uploadPath = "chat_images/${roomId}/${System.currentTimeMillis()}_$index.jpg"
                val uploadResult = fileUseCases.uploadMediaUseCase(imageUri, uploadPath)

                when (uploadResult) {
                    is CustomResult.Success -> {
                        uploadedImages.add(
                            mapOf(
                                "url" to uploadResult.data,
                                "filename" to imageUri.lastPathSegment,
                                "mime" to "image/jpeg"
                            )
                        )
                        Log.d(TAG, "✅ 이미지 ${index + 1}/${imageUris.size} 업로드 성공")
                    }

                    is CustomResult.Failure -> {
                        Log.e(TAG, "❌ 이미지 ${index + 1} 업로드 실패", uploadResult.error)
                        // 일부 실패해도 성공한 이미지들로 전송 진행
                    }

                    else -> {
                        Log.e(TAG, "❌ 이미지 ${index + 1} 업로드 알 수 없는 상태")
                    }
                }
            }

            if (uploadedImages.isNotEmpty()) {
                val payload = MessagePayload.forImages(content, uploadedImages)
                sendMessageInternal(senderId, MessageType.TEXT, payload, replyToMessageId)
            } else {
                Log.e(TAG, "❌ 모든 이미지 업로드 실패")
                CustomResult.Failure(Exception("모든 이미지 업로드에 실패했습니다"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "이미지들 메시지 전송 중 예외", e)
            CustomResult.Failure(e)
        }
    }

    /**
     * 프로젝트 멤버 초대 메시지 전송 (DM으로)
     */
    suspend fun sendMemberInvitationMessage(
        senderId: UserId,
        targetUserId: String,
        projectId: String,
        projectName: String,
        inviterName: String
    ): CustomResult<DocumentId, Exception> {
        Log.d(TAG, "멤버 초대 메시지 전송 시도: targetUserId=$targetUserId, projectId=$projectId")

        return try {
            // 현재 사용자를 위한 DM UseCase 생성
            val dmUseCases = dmUseCaseProvider.createForUser(senderId)

            // 대상 사용자의 이름을 UserName으로 변환 (실제로는 targetUserId를 사용)
            val dmFlow = dmUseCases.addDmChannelUseCase(UserName(targetUserId))
            val dmResult = dmFlow.first { it is CustomResult.Success || it is CustomResult.Failure }

            when (dmResult) {
                is CustomResult.Success -> {
                    val dmChannelId = dmResult.data.value

                    // 멤버 초대 페이로드 생성
                    val payload = MessagePayload.forMemberInvitation(
                        projectId = projectId,
                        projectName = projectName,
                        inviterName = inviterName,
                        targetUserId = targetUserId
                    )

                    Log.d(TAG, "✅ DM 채널 생성/찾기 성공: $dmChannelId")

                    // DM 채널로 멤버 초대 시스템 메시지 전송
                    // 별도의 WebSocket 인스턴스로 DM 채널에 전송
                    val dmWebSocketUseCases = webSocketUseCaseProvider.createForRoom(dmChannelId)
                    val sendResult = dmWebSocketUseCases.sendMessageUseCase(
                        senderId = senderId,
                        content = payload.value, // 시스템 메시지는 전체 payload를 content로 전송
                        messageId = DocumentId.generate(),
                        replyToMessageId = null,
                        projectId = null, // DM이므로 projectId 없음
                        channelType = "DM"
                    )

                    if (sendResult.isSuccess) {
                        Log.d(TAG, "✅ 멤버 초대 DM 메시지 전송 성공")
                        CustomResult.Success(DocumentId.generate())
                    } else {
                        Log.e(TAG, "❌ 멤버 초대 DM 메시지 전송 실패: ${sendResult.exceptionOrNull()?.message}")
                        CustomResult.Failure(Exception("DM 메시지 전송 실패"))
                    }
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "❌ DM 채널 생성 실패", dmResult.error)
                    CustomResult.Failure(dmResult.error)
                }

                else -> {
                    Log.e(TAG, "❌ DM 채널 생성 알 수 없는 상태")
                    CustomResult.Failure(Exception("DM 채널 생성 실패"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "멤버 초대 메시지 전송 중 예외", e)
            CustomResult.Failure(e)
        }
    }

    /**
     * 메시지 ACK 처리 (WebSocket ACK 수신 시)
     */
    suspend fun handleMessageAck(messageId: String) {
        try {
            val result = messageRepository.handleMessageAck(messageId)
            if (result is CustomResult.Success) {
                updateOutBoxStatusCache(messageId, OutBoxStatus.DISPATCHED)
                Log.d(TAG, "✅ 메시지 ACK 처리 완료: $messageId")
            } else {
                Log.e(TAG, "❌ 메시지 ACK 처리 실패: $result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 메시지 ACK 처리 중 예외: $messageId", e)
        }
    }

    /**
     * 메시지 실패 처리 (WebSocket 실패 수신 시)
     */
    suspend fun handleMessageFailure(messageId: String) {
        try {
            val result = messageRepository.handleMessageFailure(messageId)
            if (result is CustomResult.Success) {
                updateOutBoxStatusCache(messageId, OutBoxStatus.FAILED)
                Log.d(TAG, "✅ 메시지 실패 처리 완료: $messageId")
            } else {
                Log.e(TAG, "❌ 메시지 실패 처리 실패: $result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 메시지 실패 처리 중 예외: $messageId", e)
        }
    }

    /**
     * 로컬 Room DB에서 최신 메시지 일부를 조회하여 로그로 출력 (디버그용)
     */
    suspend fun debugLogRecentMessages(limit: Int = 10) {
        try {
            Log.d(TAG, "===== 🗂️ Room DB 최신 메시지 조회 (channel=$roomId, limit=$limit) =====")
            when (val result = messageRepository.getRecentMessages(roomId, limit)) {
                is CustomResult.Success -> {
                    val messages = result.data
                    if (messages.isEmpty()) {
                        Log.d(TAG, "(빈 목록)")
                    } else {
                        messages.forEachIndexed { index, m ->
                            val text = m.payload.getTextContent() ?: ""
                            val raw = m.payload.value.take(160).replace('\n', ' ')
                            Log.d(
                                TAG,
                                "#${index + 1} id=${m.id.value}, channel=${m.channelId.value}, at=${m.createdAt}, type=${m.messageType}, deleted=${m.isDeleted.value}, text=$text, raw=$raw"
                            )
                        }
                    }
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "Room DB 최신 메시지 조회 실패", result.error)
                }

                else -> {
                    Log.d(TAG, "Room DB 최신 메시지 조회 - 상태: $result")
                }
            }
            Log.d(TAG, "===== 🗂️ Room DB 최신 메시지 조회 끝 =====")
        } catch (e: Exception) {
            Log.e(TAG, "Room DB 최신 메시지 로그 출력 중 예외", e)
        }
    }

    /**
     * 내부 메시지 전송 로직
     */
    private suspend fun sendMessageInternal(
        senderId: UserId,
        messageType: MessageType,
        payload: MessagePayload,
        replyToMessageId: DocumentId?
    ): CustomResult<DocumentId, Exception> {
        return try {
            // 고유 메시지 ID 생성
            val messageId = DocumentId.generate()

            // 1단계: 옵티미스틱 UI - 즉시 Room DB에 저장 (전송중 상태)
            val optimisticMessage = Message.create(
                id = messageId,
                senderId = senderId,
                messageType = messageType,
                payload = payload,
                replyToMessageId = replyToMessageId,
                mentions = emptyList(),
                channelId = ChannelId(roomId)
            )

            // Room DB에 즉시 저장 (UI에 즉시 표시)
            messageRepository.save(optimisticMessage)
            Log.d(TAG, "✅ 옵티미스틱 메시지 Room DB 저장 완료: ${messageId.value}")

            // OutBox 레코드 생성 (PENDING 상태로 전송 대기)
            val result = messageRepository.createOutBoxRecord(
                messageId = messageId.value,
                channelId = roomId,
                payload = payload.value
            )

            if (result is CustomResult.Success) {
                // OutBox 상태 캐시 업데이트 (PENDING)
                updateOutBoxStatusCache(messageId.value, OutBoxStatus.PENDING)
                Log.d(TAG, "✅ OutBox 레코드 생성 완료: ${messageId.value}")
            } else {
                Log.e(TAG, "❌ OutBox 레코드 생성 실패: ${result}")
            }

            // 2단계: 백그라운드 WebSocket 전송
            val contentForWebSocket = when (messageType) {
                MessageType.TEXT -> payload.getTextContent() ?: ""
                // 시스템/이미지/복합 페이로드는 전체 JSON 문자열을 전송
                else -> payload.value
            }
            
            val sendResult = roomWebSocketUseCases.sendMessageUseCase(
                senderId = senderId,
                content = contentForWebSocket,
                messageId = messageId,
                replyToMessageId = replyToMessageId,
                projectId = projectId,
                channelType = channelType
            )

            if (sendResult.isSuccess) {
                Log.d(TAG, "✅ WebSocket 메시지 전송 성공: ${messageId.value}")
                CustomResult.Success(messageId)
            } else {
                Log.e(TAG, "❌ WebSocket 메시지 전송 실패: ${sendResult.exceptionOrNull()?.message}")

                // 오프라인 큐에 추가 (재전송용)
                offlineMessageQueue.queueMessage(
                    com.example.feature_chat.queue.QueuedMessageAction.Send(
                        message = optimisticMessage,
                        roomId = roomId
                    )
                )

                CustomResult.Success(messageId)
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

                // Room DB에서 메시지 업데이트
                val existingMessage = messageRepository.findById(messageId)
                if (existingMessage is CustomResult.Success) {
                    val newPayload = MessagePayload.forText(newContent)
                    existingMessage.data.updatePayload(newPayload)
                    messageRepository.save(existingMessage.data)
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

                // Room DB에서 메시지 삭제 마킹
                val existingMessage = messageRepository.findById(messageId)
                if (existingMessage is CustomResult.Success) {
                    existingMessage.data.delete()
                    messageRepository.save(existingMessage.data)
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
}