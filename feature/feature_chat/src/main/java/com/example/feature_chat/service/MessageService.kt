package com.example.feature_chat.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.core_common.constants.PagingConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.CustomResult.Success
import com.example.core_common.util.AuthUtil
import com.example.core_common.util.DateTimeUtil
import com.example.core_common.util.ImageCompressor
import com.example.domain.enum.OutBoxStatus
import com.example.domain.model.base.Message
import com.example.domain.vo.ChannelId
import com.example.domain.vo.CollectionPath
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import com.example.domain_repository.base.MessageRepository
import com.example.domain_usecase.provider.file.FileManagementUseCases
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.model.MessageDeliveryState
import com.example.websocket.usecase.SendMessageUseCase
import com.example.websocket.usecase.WebSocketUseCaseProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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
    private val context: Context,
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    private val messageRepository: MessageRepository,
    private val userProfileService: UserProfileService,
    private val fileUseCases: FileManagementUseCases,
    private val sendMessageUseCase: SendMessageUseCase,
    private val roomId: String,
    private val projectId: String? = null
) {

    companion object {
        private const val TAG = "MessageService"
    }

    // URI-Storage 경로 매핑 캐시 (로컬 URI -> Firebase Storage 경로)
    private val uriToStoragePathCache = mutableMapOf<String, String>()


    init {
        // Repository 컬렉션 컨텍스트 설정 누락으로 인한 오류 방지
        try {
            if (!projectId.isNullOrBlank()) {
                // 프로젝트 채널: projects/{projectId}/channels/{channelId}/messages
                messageRepository.setCollection(
                    CollectionPath.projectChannelMessages(projectId, roomId)
                )
            } else {
                // DM 채널: dm_channels/{channelId}/messages
                messageRepository.setCollection(CollectionPath.dmChannelMessages(roomId))
            }
            Log.d(TAG, "Repository 컬렉션 설정 완료: roomId=$roomId, projectId=$projectId")
        } catch (e: Exception) {
            Log.e(TAG, "Repository 컬렉션 설정 실패", e)
        }
    }

    // OutBox 상태는 캐시를 사용하지 않고 Room OutBoxEntity를 통해 관찰/조회합니다.

    // WebSocket 사용 사례 (방별) - 메시지 전송용으로만 사용
    private val roomWebSocketUseCases = webSocketUseCaseProvider.createForRoom(roomId)


    // ================================
    // 메시지 읽기 (Paging3) - UI 모델 변환 포함
    // ================================
    


    /**
     * UI에서 직접 사용할 수 있도록 UI 모델로 변환된 Paging3 스트림 제공
     * @param initialMessageId 앵커 메시지 ID (null이면 최신 메시지부터 시작)
     * @param useFallbackAnchor 앵커 메시지가 없을 때 현재 시간을 fallback으로 사용할지 여부
     */
    fun getUiMessagesPagingFlow(
        initialMessageId: String? = null,
        useFallbackAnchor: Boolean = false
    ): Flow<PagingData<ChatMessageUiModel>> {
        Log.d(TAG, "UI 메시지 Paging Flow 제공 - initialMessageId=$initialMessageId, useFallbackAnchor=$useFallbackAnchor")

        val initialKey: Int? = try {
            if (initialMessageId.isNullOrBlank()) {
                if (useFallbackAnchor) {
                    System.currentTimeMillis().toInt()
                } else {
                    null
                }
            } else {
                val anchor = runBlocking { messageRepository.findById(initialMessageId) }
                if (anchor != null) {
                    anchor.createdAt?.toEpochMilli()?.toInt()
                } else if (useFallbackAnchor) {
                    Log.d(TAG, "앵커 메시지 미존재, fallback 사용: $initialMessageId")
                    System.currentTimeMillis().toInt()
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            if (useFallbackAnchor) System.currentTimeMillis().toInt() else null
        }

        val pager = Pager(
            config = PagingConfig(
                pageSize = PagingConstants.PAGE_SIZE,
                initialLoadSize = PagingConstants.INITIAL_LOAD_SIZE,
                prefetchDistance = PagingConstants.PREFETCH_DISTANCE,
                maxSize = PagingConstants.MAX_SIZE,
                enablePlaceholders = false
            ),
            initialKey = initialKey,
            pagingSourceFactory = {
                messageRepository.getMessageEntityPagingSource(roomId)
            }
        )

        // Message Paging Flow를 도메인 변환하여 반환 (Flow combination 없이 직접 변환)
        return pager.flow.map { pagingData ->
            pagingData.map { entity ->
                val message = messageRepository.convertEntityToDomain(entity)
                // OutBox 상태는 현재 시점에서 동기적으로 조회 (성능을 위해)
                val outboxStatus = try {
                    runBlocking {
                        val result = messageRepository.getMessageOutBoxStatus(message.id)
                        if (result is Success) result.data else null
                    }
                } catch (e: Exception) {
                    null
                }
                convertDomainMessageToUiModel(message, outboxStatus)
            }
        }
    }

    /**
     * 도메인 Message를 ChatMessageUiModel로 변환
     * 캐싱을 통한 성능 최적화
     * @param message 도메인 메시지 객체
     * @param outboxStatus OutBox 상태 (null이면 정상 상태로 간주)
     */
    fun convertDomainMessageToUiModel(
        message: Message,
        outboxStatus: OutBoxStatus? = null
    ): ChatMessageUiModel {
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

        // OutBox 상태 기반 단순 상태 계산 (내가 보낸 메시지이면서 OutBox가 있을 때만)
        val isSending = (senderId == currentUserId) && (outboxStatus == OutBoxStatus.PENDING)
        val sendFailed = (senderId == currentUserId) && (outboxStatus == OutBoxStatus.FAILED)

        // 이미지 URL 추출 (OutBox와 독립적)
        val imageUrls = extractImageUrlsFromPayload(message.payload.value)
        val hasImages = imageUrls.isNotEmpty()

        // 메시지 수정 여부 확인 (payload의 _meta에서 pendingOp:edit 확인)
        val isModified = try {
            val messagePayload = MessagePayload(message.payload.value)
            messagePayload.getMetaPendingOp() == "edit"
        } catch (e: Exception) {
            // payload 파싱 실패 시 타임스탬프로 fallback
            kotlin.math.abs(message.createdAt.epochSecond - message.updatedAt.epochSecond) > 1
        }

        return ChatMessageUiModel(
            messageId = message.id.value,
            userId = senderId,
            userName = userProfileService.getUserDisplayName(senderId),
            userProfileUrl = userProfileService.getCachedProfileUrl(senderId),
            messageType = message.messageType,
            message = message.payload.getTextContent() ?: "",
            // UI 모델은 JSON 문자열을 기대하므로 value를 전달 (toString()은 'MessagePayload(value=...)' 형식이라 파싱 실패)
            payload = message.payload.value,
            formattedTimestamp = DateTimeUtil.formatChatTime(message.createdAt),
            actualTimestamp = message.createdAt,
            isModified = isModified,
            attachmentImageUrls = emptyList(), // 레거시 - 하위 호환용
            imageUrls = imageUrls, // 새로운 이미지 URL 목록
            hasImages = hasImages, // 이미지 포함 여부
            isMyMessage = senderId == currentUserId,
            isSending = isSending, // OutBox 상태만 확인
            sendFailed = sendFailed,
            isDeleted = message.isDeleted.value,
            deliveryState = when (outboxStatus) {
                OutBoxStatus.PENDING -> MessageDeliveryState.Sending
                OutBoxStatus.FAILED -> MessageDeliveryState.Failed("send_failed")
                else -> MessageDeliveryState.Sent
            },
            isOptimistic = false, // OutBox 중심 처리로 단순화
            clientSentAt = null,
            retryCount = 0,
            canRetry = outboxStatus == OutBoxStatus.FAILED,
            errorMessage = null,
            replyToMessageId = message.replyToMessageId?.value,
            replyToContent = null, // TODO: 답장 대상 메시지 내용 조회
            replyToUserName = null, // TODO: 답장 대상 사용자 이름 조회
            mentions = message.mentions,
            isMentionedMessage = false // TODO: 현재 사용자 멘션 여부 확인
        )
    }

    // ================================
    // OutBox 기반 상태 관찰 헬퍼 (UI 바인딩용)
    // ================================

    fun observeMessageDeliveryStatus(messageId: String): Flow<MessageDeliveryState> {
        return messageRepository
            .observeMessageOutBoxStatus(DocumentId(messageId))
            .map { status ->
                when (status) {
                    OutBoxStatus.PENDING -> MessageDeliveryState.Sending
                    OutBoxStatus.DISPATCHED -> MessageDeliveryState.Sent
                    OutBoxStatus.FAILED -> MessageDeliveryState.Failed("send_failed")
                }
            }
    }

    fun observeChannelPendingCount(): Flow<Int> =
        messageRepository.observeChannelPendingCount(roomId)

    /**
     * 채널 내 각 메시지의 OutBox 상태 맵을 관찰합니다. (messageId -> OutBoxStatus)
     * UI가 개별 row 로딩/실패 인디케이터를 바인딩할 때 사용할 수 있습니다.
     */
    fun observeChannelOutBoxStatuses(): Flow<Map<String, OutBoxStatus>> =
        messageRepository.observeChannelOutBoxStatuses(roomId)

    private suspend fun loadWithTimeout(userId: String) {
        // 150ms 내에 프로필 불러오기 시도 (캐시 미스 시만)
        try {
            kotlinx.coroutines.withTimeout(400) {
                userProfileService.loadUserProfile(userId)
            }
        } catch (_: Exception) { /* timeout or error - fallback to unknown */
        }
    }

    /**
     * 메시지 payload에서 이미지 URL들을 추출하는 유틸리티 메서드
     * MessagePayload 클래스의 메서드를 활용하여 정확한 구조 파싱
     */
    private fun extractImageUrlsFromPayload(payload: String): List<String> {
        return try {
            val messagePayload = MessagePayload(payload)
            val attachments = messagePayload.getAttachments()
            val imageUrls = attachments
                .filter { (it[MessagePayload.KEY_KIND] as? String) == "image" }
                .mapNotNull { it[MessagePayload.KEY_URL] as? String }

            // 로그 최소화: 이미지가 있는 경우에만 payload만 출력하고, 개수/세부 정보는 생략
            if (imageUrls.isNotEmpty()) {
                Log.d(TAG, "🖼️ payload=$payload")
            }

            imageUrls
        } catch (e: Exception) {
            Log.e(TAG, "❌ [이미지 URL 파싱 실패] ${e.message}", e)
            Log.e(TAG, "❌ 실패한 payload: $payload")
            emptyList()
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
            // 1) 메시지 타임스탬프 조회 (로컬 전용 findById 사용)
            val anchor = messageRepository.findById(messageId)
            val anchorTsMs = anchor?.createdAt?.toEpochMilli()
                ?: System.currentTimeMillis()

            // 2) 앵커 주변 범위 프리페치 (로컬 캐시 기준)
            runCatching {
                messageRepository.getMessagesBefore(roomId, anchorTsMs, PagingConstants.PAGE_SIZE)
            }
            runCatching {
                messageRepository.getMessagesAfter(roomId, anchorTsMs, PagingConstants.PAGE_SIZE)
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


    // ================================
    // 통합 메시지 전송 (단일 진입점)
    // ================================
    
    /**
     * 통합 메시지 전송 메서드 - 단순한 이미지 업로드 플로우
     *
     * 프로세스:
     * 1. 이미지 선택 시 URI와 Storage 경로 매핑
     * 2. 업로딩 상태로 payload 생성 (placeholder)
     * 3. WebSocket으로 즉시 전송
     * 4. 메시지 전송 성공 시 백그라운드에서 Firebase Storage 업로드
     * 5. 업로드 완료 후 payload 업데이트
     *
     * @param senderId 발신자 ID
     * @param textContent 텍스트 내용 (비어있을 수 있음)
     * @param imageUris 이미지 URI 리스트 (비어있을 수 있음)
     * @param replyToMessageId 답장 대상 메시지 ID (옵션)
     * @param isSystemMessage 시스템 메시지 여부 (기본값: false)
     * @param systemType 시스템 메시지 타입 (시스템 메시지인 경우 필요)
     * @param additionalMetadata 추가 메타데이터
     * @return 생성된 메시지 ID 또는 오류
     */
    suspend fun sendMessage(
        senderId: UserId,
        textContent: String = "",
        imageUris: List<Uri> = emptyList(),
        replyToMessageId: DocumentId? = null,
        isSystemMessage: Boolean = false,
        systemType: String? = null,
        additionalMetadata: Map<String, String> = emptyMap(),
        mentions: List<com.example.domain.vo.message.MentionInfo> = emptyList()
    ): CustomResult<DocumentId, Exception> {
        Log.d(TAG, "메시지 전송 시도: text='$textContent', images=${imageUris.size}개")
        
        return try {
            // 🎯 중앙화된 MessageId 생성 (전체 플로우에서 단일 ID 사용)
            val messageId = DocumentId(java.util.UUID.randomUUID().toString())
            Log.d(TAG, "생성된 MessageId: ${messageId.value}")
            
            // 이미지가 있는 경우 단순한 placeholder 플로우 사용
            if (imageUris.isNotEmpty() && !isSystemMessage) {
                Log.d(TAG, "📸 [이미지전송] 이미지 메시지 플로우 시작: ${imageUris.size}개 이미지")
                return sendImageMessageSimple(
                    messageId = messageId,
                    senderId = senderId,
                    textContent = textContent,
                    imageUris = imageUris,
                    replyToMessageId = replyToMessageId,
                    mentions = mentions
                )
            }

            // 일반 텍스트/시스템 메시지 처리 - 도메인 Message 구성 후 통합 UseCase 호출
            val (messagePayload, actualMessageType) = if (isSystemMessage && systemType != null) {
                // 시스템 메시지 payload 구성
                val jsonObject = buildJsonObject {
                    put(MessagePayload.KEY_CONTENT, JsonPrimitive(textContent))
                    additionalMetadata.forEach { (key, value) ->
                        put(key, JsonPrimitive(value))
                    }
                }
                val payload = MessagePayload(jsonObject.toString())

                // systemType에 따른 MessageType 매핑
                val messageType = when (systemType) {
                    "USER_INVITE", "MEMBER_INVITATION" -> {
                        // projectId, projectName, inviterName, targetUserId 검증
                        if (additionalMetadata.containsKey("projectId") &&
                            additionalMetadata.containsKey("projectName") &&
                            additionalMetadata.containsKey("inviterName")
                        ) {
                            MessageType.SYSTEM_MEMBER_INVITATION
                        } else {
                            MessageType.TEXT
                        }
                    }

                    "PROJECT_INVITE" -> {
                        // projectId, projectName, inviterName, invitationId 검증
                        if (additionalMetadata.containsKey("projectId") &&
                            additionalMetadata.containsKey("projectName") &&
                            additionalMetadata.containsKey("inviterName")
                        ) {
                            MessageType.PROJECT_INVITE
                        } else {
                            MessageType.TEXT
                        }
                    }

                    "PROJECT_JOIN" -> MessageType.SYSTEM_PROJECT_JOIN
                    "PROJECT_LEAVE" -> MessageType.SYSTEM_PROJECT_LEAVE
                    "DATE" -> MessageType.SYSTEM_DATE
                    else -> MessageType.TEXT
                }

                payload to messageType
            } else {
                MessagePayload.forText(textContent) to MessageType.TEXT
            }

            val domainMessage = Message.create(
                id = messageId,
                senderId = senderId,
                messageType = actualMessageType,
                payload = messagePayload,
                replyToMessageId = replyToMessageId,
                mentions = mentions,
                channelId = ChannelId(roomId)
            )

            val result = sendMessageUseCase(
                message = domainMessage,
                projectId = projectId?.let { com.example.domain.vo.ProjectId(it) }
            )


            result

        } catch (e: Exception) {
            Log.e(TAG, "메시지 전송 중 예외", e)
            CustomResult.Failure(e)
        }
    }


    /**
     * 단순한 이미지 메시지 전송 (새로운 방식)
     * 1. URI와 Storage 경로 매핑 저장
     * 2. Placeholder payload로 즉시 전송
     * 3. 메시지 전송 성공 시 백그라운드 업로드 시작
     */
    private suspend fun sendImageMessageSimple(
        messageId: DocumentId,
        senderId: UserId,
        textContent: String,
        imageUris: List<Uri>,
        replyToMessageId: DocumentId?,
        mentions: List<com.example.domain.vo.message.MentionInfo> = emptyList()
    ): CustomResult<DocumentId, Exception> {
        return try {
            Log.d(TAG, "🖼️ 이미지 메시지 전송 시작: messageId=${messageId.value}, images=${imageUris.size}개")

            // 1단계: URI와 Storage 경로 매핑 저장
            val uriToPathMapping = mutableMapOf<String, String>()
            imageUris.forEachIndexed { index, uri ->
                val ext = ImageCompressor.getExtension(context, uri) ?: "jpg"
                val fileName = "${messageId.value}_${index}.${ext}"
                val storagePath =
                    CollectionPath.storageMessageAttachment(roomId, messageId.value, fileName).value

                uriToPathMapping[uri.toString()] = storagePath
                uriToStoragePathCache[uri.toString()] = storagePath
            }
            Log.d(
                TAG,
                "📂 [이미지전송] Storage 경로 매핑 완료: ${uriToPathMapping.size}개 URI → Firebase Storage 경로"
            )

            // 2단계: 로컬 DB에 placeholder 저장 (UI 즉시 표시용, uploading true + progress 0)
            val placeholderPayload = if (imageUris.size == 1) {
                MessagePayload.forImagePlaceholder(
                    content = textContent,
                    localUri = imageUris.first().toString()
                )
            } else {
                MessagePayload.forImagePlaceholders(
                    content = textContent,
                    localUris = imageUris.map { it.toString() }
                )
            }

            // 로컬 DB에 저장
            val placeholderMessage = Message.create(
                id = messageId,
                senderId = senderId,
                messageType = MessageType.TEXT,
                payload = placeholderPayload,
                replyToMessageId = replyToMessageId,
                mentions = mentions,
                channelId = ChannelId(roomId)
            )

            val saveResult = messageRepository.sendMessage(placeholderMessage)
            if (saveResult.isFailure) {
                return CustomResult.Failure(Exception("메시지 로컬 저장 실패"))
            }

            // OutBox 상태는 save()/enqueue 단계에서 PENDING으로 관리됩니다.

            // 3단계: 업로드 선행(Option A) - 모든 이미지를 Firebase Storage에 업로드하여 안정 URL 획득
            Log.d(TAG, "🔄 [이미지전송] Firebase Storage 업로드(선행) 시작")
            val uploadedImages = mutableListOf<Map<String, Any?>>()
            imageUris.forEachIndexed { index, uri ->
                val storagePath = uriToPathMapping[uri.toString()] ?: return@forEachIndexed
                val uploadResult = fileUseCases.uploadFileUseCase(uri, storagePath)
                if (uploadResult is Success) {
                    val ext = ImageCompressor.getExtension(context, uri) ?: "jpg"
                    val mime = if (ext == "jpg") "image/jpeg" else "image/$ext"
                    uploadedImages.add(
                        mapOf(
                            "kind" to "image",
                            "index" to index,
                            "url" to uploadResult.data,
                            "filename" to (uri.lastPathSegment ?: "image.$ext"),
                            "mime" to mime
                        )
                    )
                } else {
                    Log.e(TAG, "❌ 업로드 실패(선행): ${index + 1}/${imageUris.size}")
                }
            }

            if (uploadedImages.isEmpty()) {
                Log.e(TAG, "❌ 모든 이미지 업로드 실패(선행)")
                markMessageAsFailed(messageId)
                return CustomResult.Failure(Exception("모든 이미지 업로드 실패"))
            }

            // 4단계: 첨부 포함 payload 구성
            val finalPayload = MessagePayload.forImages(
                content = textContent,
                images = uploadedImages
            )

            // 5단계: 통합 UseCase로 메시지 전송 (로컬 업서트 + OutBox + WebSocket)
            val domainMessage = Message.create(
                id = messageId,
                senderId = senderId,
                messageType = MessageType.TEXT,
                payload = finalPayload,
                replyToMessageId = replyToMessageId,
                mentions = mentions,
                channelId = ChannelId(roomId)
            )

            val result = sendMessageUseCase(
                message = domainMessage,
                projectId = projectId?.let { com.example.domain.vo.ProjectId(it) }
            )

            if (result is CustomResult.Failure) {
                Log.e(TAG, "❌ 첨부 포함 메시지 전송 실패", result.error)
                markMessageAsFailed(messageId)
            } else {
                Log.d(TAG, "✅ 첨부 포함 메시지 전송 완료: ${messageId.value}")
            }

            result

        } catch (e: Exception) {
            Log.e(TAG, "❌ 이미지 메시지 전송 중 예외", e)
            CustomResult.Failure(e)
        }
    }

    /**
     * 백그라운드에서 Firebase Storage에 이미지 업로드 수행
     */
    private suspend fun startBackgroundImageUpload(
        messageId: DocumentId,
        imageUris: List<Uri>,
        uriToPathMapping: Map<String, String>,
        textContent: String
    ) {
        try {
            Log.d(TAG, "🔄 백그라운드 이미지 업로드 시작: messageId=${messageId.value}")

            val uploadedImages = mutableListOf<Map<String, Any?>>()

            // 각 이미지를 순차적으로 업로드
            imageUris.forEachIndexed { index, uri ->
                try {
                    val storagePath = uriToPathMapping[uri.toString()]
                    if (storagePath.isNullOrBlank()) {
                        Log.e(TAG, "❌ 이미지 ${index + 1}: Storage 경로를 찾을 수 없음")
                        return@forEachIndexed
                    }

                    // ImageCompressor로 압축
                    val compressedUri = ImageCompressor.compressImage(
                        context = context,
                        imageUri = uri,
                        options = ImageCompressor.CompressionOptions(
                            maxWidth = 1920,
                            maxHeight = 1920,
                            quality = 85,
                            maxFileSizeBytes = 3L * 1024L * 1024L // 3MB
                        )
                    )

                    // FileRepository를 통해 Firebase Storage 업로드
                    val uploadResult = fileUseCases.uploadFileUseCase(compressedUri, storagePath)

                    when (uploadResult) {
                        is Success -> {
                            Log.d(
                                TAG,
                                "✅ [Firebase저장] 이미지 ${index + 1}/${imageUris.size} Firebase Storage 저장 완료"
                            )

                            val ext = ImageCompressor.getExtension(context, uri) ?: "jpg"
                            val mime = if (ext == "jpg") "image/jpeg" else "image/$ext"

                            uploadedImages.add(
                                mapOf(
                                    "kind" to "image",
                                    "index" to index,
                                    "url" to uploadResult.data,
                                    "filename" to (uri.lastPathSegment ?: "image.$ext"),
                                    "mime" to mime,
                                    "ext" to ext
                                )
                            )
                        }

                        is CustomResult.Failure -> {
                            Log.e(
                                TAG,
                                "❌ 이미지 ${index + 1}/${imageUris.size} 업로드 실패",
                                uploadResult.error
                            )
                            // 실패한 이미지는 건너뛰고 계속 진행
                        }

                        else -> {
                            Log.w(TAG, "⚠️ 이미지 ${index + 1}/${imageUris.size} 업로드 알 수 없는 상태")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 이미지 ${index + 1}/${imageUris.size} 업로드 중 예외", e)
                }
            }

            // 업로드 완료된 이미지들로 payload 업데이트
            if (uploadedImages.isNotEmpty()) {
                updateMessagePayloadWithImages(messageId, textContent, uploadedImages)
            } else {
                Log.e(TAG, "❌ 모든 이미지 업로드 실패, 메시지를 실패 상태로 표시")
                markMessageAsFailed(messageId)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ 백그라운드 이미지 업로드 중 예외", e)
            markMessageAsFailed(messageId)
        }
    }

    /**
     * 이미지 업로드 완료 후 메시지 payload를 실제 Firebase URL로 업데이트
     */
    private suspend fun updateMessagePayloadWithImages(
        messageId: DocumentId,
        textContent: String,
        uploadedImages: List<Map<String, Any?>>
    ) {
        try {
            Log.d(
                TAG,
                "🔄 메시지 payload 업데이트: messageId=${messageId.value}, images=${uploadedImages.size}개"
            )

            // 업로드된 이미지 데이터로 최종 payload 생성
            val finalPayload = MessagePayload.forImages(
                content = textContent,
                images = uploadedImages
            )

            // 로컬 DB의 메시지 payload 업데이트 (로컬 전용 findById 사용)
            val message = messageRepository.findById(messageId.value)
            if (message != null) {
                message.updatePayload(finalPayload)

                val saveResult = messageRepository.sendMessage(message)
                if (saveResult.isSuccess) {
                    Log.d(
                        TAG,
                        "✅ [이미지전송완료] 메시지 payload를 실제 Firebase URL로 업데이트 완료: ${messageId.value}"
                    )
                    // OutBox 상태 변경은 WebSocket ACK 처리에 의해 갱신됩니다.
                } else {
                    Log.e(TAG, "❌ 메시지 payload 업데이트 저장 실패")
                    markMessageAsFailed(messageId)
                }
            } else {
                Log.e(TAG, "❌ 업데이트할 메시지를 찾을 수 없음: ${messageId.value}")
                markMessageAsFailed(messageId)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ 메시지 payload 업데이트 중 예외", e)
            markMessageAsFailed(messageId)
        }
    }
    /**
     * 메시지 ACK 처리 (WebSocket ACK 수신 시)
     */
    suspend fun handleMessageAck(messageId: String) {
        try {
            val result = messageRepository.handleMessageAck(messageId)
            if (result is Success) {
                // Room이 자동으로 invalidation을 처리하므로 수동 invalidation 불필요
                Log.d(TAG, "✅ 메시지 ACK 처리 완료 + UI 갱신: $messageId")
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
            if (result is Success) {
                // OutBox 상태는 handleMessageFailure에서 자동으로 FAILED 상태로 업데이트됩니다

                // Room이 자동으로 invalidation을 처리하므로 수동 invalidation 불필요

                Log.d(TAG, "✅ 메시지 실패 처리 완료 + UI 갱신: $messageId")
            } else {
                Log.e(TAG, "❌ 메시지 실패 처리 실패: $result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 메시지 실패 처리 중 예외: $messageId", e)
        }
    }


    /**
     * 내부 메시지 전송 로직 (payload 기반)
     */
    private suspend fun sendMessageInternal(
        messageId: DocumentId,
        senderId: UserId,
        messageType: MessageType,
        payload: MessagePayload,
        replyToMessageId: DocumentId?,
        mentions: List<com.example.domain.vo.message.MentionInfo> = emptyList()
    ): CustomResult<DocumentId, Exception> {
        return try {
            val domainMessage = Message.create(
                id = messageId,
                senderId = senderId,
                messageType = messageType,
                payload = payload,
                replyToMessageId = replyToMessageId,
                mentions = mentions,
                channelId = ChannelId(roomId)
            )

            val result = sendMessageUseCase(
                message = domainMessage,
                projectId = projectId?.let { com.example.domain.vo.ProjectId(it) }
            )


            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ sendMessageInternal 중 예외", e)
            CustomResult.Failure(e)
        }
    }

    private fun mapToJsonString(map: Map<String, Any?>): String {
        fun escape(s: String): String = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        fun toJson(value: Any?): String = when (value) {
            null -> "null"
            is String -> "\"${escape(value)}\""
            is Number, is Boolean -> value.toString()
            is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
                val key = k?.toString() ?: "null"
                "\"${escape(key)}\":" + toJson(v)
            }
            is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { elem -> toJson(elem) }
            else -> "\"${escape(value.toString())}\""
        }

        return toJson(map)
    }
    
    /**
     * 메시지 수정 - WebSocket UseCase 사용
     */
    suspend fun editMessage(
        messageId: DocumentId,
        newContent: String
    ): CustomResult<Unit, Exception> {
        Log.d(TAG, "메시지 수정(낙관적) 시도: messageId=${messageId.value}")

        // 1) 로컬 낙관적 적용 (백업 포함)
        applyOptimisticEdit(messageId, newContent)

        // 2) WebSocket 전송
        return try {
            // 기존 메시지에서 attachments 보존하면서 content만 업데이트
            val newPayload = createUpdatedPayloadWithPreservedAttachments(messageId, newContent)
            val editResult = roomWebSocketUseCases.editMessageUseCase(
                messageId = messageId,
                newPayload = newPayload
            )

            if (editResult.isSuccess) {
                Log.d(TAG, "메시지 수정 전송 성공(ACK 대기): ${messageId.value}")
                Success(Unit)
            } else {
                Log.e(TAG, "메시지 수정 전송 실패: ${editResult.exceptionOrNull()?.message}")
                // 즉시 롤백
                revertOptimisticEdit(messageId.value)
                CustomResult.Failure(Exception(editResult.exceptionOrNull()))
            }
        } catch (e: Exception) {
            // 전송 예외 시 롤백
            Log.e(TAG, "메시지 수정 전송 중 예외", e)
            revertOptimisticEdit(messageId.value)
            CustomResult.Failure(e)
        }
    }
    
    /**
     * 메시지 삭제 - WebSocket UseCase 사용
     */
    suspend fun deleteMessage(messageId: DocumentId): CustomResult<Unit, Exception> {
        Log.d(TAG, "메시지 삭제(낙관적) 시도: messageId=${messageId.value}")

        // 1) 로컬 낙관적 삭제 적용 (백업 포함)
        applyOptimisticDelete(messageId)

        // 2) WebSocket 전송
        return try {
            val deleteResult = roomWebSocketUseCases.deleteMessageUseCase(
                messageId = messageId
            )

            if (deleteResult.isSuccess) {
                Log.d(TAG, "메시지 삭제 전송 성공(ACK 대기): ${messageId.value}")
                Success(Unit)
            } else {
                Log.e(TAG, "메시지 삭제 전송 실패: ${deleteResult.exceptionOrNull()?.message}")
                // 즉시 롤백
                revertOptimisticDelete(messageId.value)
                CustomResult.Failure(Exception(deleteResult.exceptionOrNull()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "메시지 삭제 전송 중 예외", e)
            revertOptimisticDelete(messageId.value)
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 🎯 에러 처리 및 재시도 유틸리티
    // ================================

    /**
     * 재시도 가능한 에러인지 확인
     */
    private fun isRetryableError(error: Throwable): Boolean {
        return when (error) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is java.net.UnknownHostException,
            is java.io.IOException -> true

            else -> {
                val message = error.message?.lowercase() ?: ""
                message.contains("timeout") ||
                        message.contains("network") ||
                        message.contains("connection") ||
                        message.contains("firebase") && message.contains("storage")
            }
        }
    }

    // ================================
    // ✏️🗑️ 낙관적 편집/삭제 지원 메서드
    // ================================

    /** 로컬에 낙관적 편집 적용 (없으면 업서트) */
    suspend fun applyOptimisticEdit(messageId: DocumentId, newContent: String) {
        try {
            val existing = messageRepository.findById(messageId.value)
            if (existing != null) {
                val currentPayload = existing.payload
                // 기존 attachments 보존 + content 교체
                val preservedAttachments = currentPayload.getAttachments()
                val updatedPayload = if (preservedAttachments.isNotEmpty()) {
                    MessagePayload.forTextWithAttachments(newContent, preservedAttachments)
                } else {
                    MessagePayload.forText(newContent)
                }
                // 낙관적 메타 추가
                val optimisticPayload = MessagePayload(updatedPayload.value)
                    .addOptimisticEdit(currentPayload.value)
                existing.updatePayload(optimisticPayload)
                messageRepository.sendMessage(existing)
            } else {
                // 업서트: 없는 경우 임시 메시지 생성
                val temp = Message.create(
                    id = messageId,
                    senderId = UserId(AuthUtil.getCurrentUserId()),
                    messageType = MessageType.TEXT,
                    payload = MessagePayload.forText(newContent).addOptimisticEdit("{}"),
                    replyToMessageId = null,
                    mentions = emptyList(),
                    channelId = ChannelId(roomId)
                )
                messageRepository.sendMessage(temp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "applyOptimisticEdit 실패", e)
        }
    }

    /** 로컬에 낙관적 삭제 적용 (없으면 업서트) */
    suspend fun applyOptimisticDelete(messageId: DocumentId) {
        try {
            val existing = messageRepository.findById(messageId.value)
            if (existing != null) {
                val cur = existing
                val newPayload = cur.payload.addOptimisticDelete(cur.payload.value)
                cur.updatePayload(newPayload)
                cur.delete()
                messageRepository.sendMessage(cur)
            } else {
                // 업서트: 없는 경우 삭제된 임시 메시지 생성
                val temp = Message.create(
                    id = messageId,
                    senderId = UserId(AuthUtil.getCurrentUserId() ?: ""),
                    messageType = MessageType.TEXT,
                    payload = MessagePayload.forText("").addOptimisticDelete("{}"),
                    replyToMessageId = null,
                    mentions = emptyList(),
                    channelId = ChannelId(roomId)
                )
                temp.delete()
                messageRepository.sendMessage(temp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "applyOptimisticDelete 실패", e)
        }
    }

    /** ACK 수신 시 낙관적 메타 정리(편집/삭제 공통) */
    suspend fun handleOptimisticAck(messageId: String) {
        try {
            val cur = messageRepository.findById(messageId)
            if (cur != null) {
                val cleaned = MessagePayload(cur.payload.value).clearOptimisticMeta()
                cur.updatePayload(cleaned)
                messageRepository.sendMessage(cur)

                // Room이 자동으로 invalidation을 처리하므로 수동 invalidation 불필요
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleOptimisticAck 실패: $messageId", e)
        }
    }

    /** FAILED 처리 시 낙관적 롤백(편집/삭제 공통) */
    suspend fun handleOptimisticFailure(messageId: String) {
        try {
            val cur = messageRepository.findById(messageId)
            if (cur != null) {
                val (pendingOp, _) = Pair(
                    MessagePayload(cur.payload.value).getOptimisticOp(),
                    MessagePayload(cur.payload.value).getOptimisticBackup()
                )
                when (pendingOp) {
                    MessagePayload.OP_EDIT -> revertOptimisticEdit(messageId)
                    MessagePayload.OP_DELETE -> revertOptimisticDelete(messageId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleOptimisticFailure 실패: $messageId", e)
        }
    }

    suspend fun revertOptimisticEdit(messageId: String) {
        try {
            val cur = messageRepository.findById(messageId)
            if (cur != null) {
                val (_, backupJson) = Pair(
                    MessagePayload(cur.payload.value).getOptimisticOp(),
                    MessagePayload(cur.payload.value).getOptimisticBackup()
                )
                val backup = backupJson ?: return
                // 백업으로 복원하고 메타 제거
                val restoredPayload = try {
                    // 백업이 JSON이면 그대로
                    Json.parseToJsonElement(backup); backup
                } catch (_: Exception) {
                    // 텍스트만 있는 경우
                    MessagePayload.forText(backup).value
                }
                val cleaned = MessagePayload(restoredPayload).clearOptimisticMeta().value
                cur.updatePayload(MessagePayload(cleaned))
                messageRepository.sendMessage(cur)
            } else {
                // 없던 메시지면 단순 제거
                messageRepository.delete(DocumentId(messageId))
            }
        } catch (e: Exception) {
            Log.e(TAG, "revertOptimisticEdit 실패: $messageId", e)
        }
    }

    suspend fun revertOptimisticDelete(messageId: String) {
        try {
            val cur = messageRepository.findById(messageId)
            if (cur != null) {
                val (_, backupJson) = Pair(
                    MessagePayload(cur.payload.value).getOptimisticOp(),
                    MessagePayload(cur.payload.value).getOptimisticBackup()
                )
                val restoredPayload = when {
                    backupJson == null -> cur.payload.value
                    else -> try {
                        Json.parseToJsonElement(backupJson); backupJson
                    } catch (_: Exception) {
                        MessagePayload.forText(backupJson).value
                    }
                }
                // 삭제 해제는 재구성하여 저장
                val rebuilt = Message.fromDataSource(
                    id = cur.id,
                    senderId = cur.senderId,
                    messageType = cur.messageType,
                    payload = MessagePayload(restoredPayload).clearOptimisticMeta(),
                    replyToMessageId = cur.replyToMessageId,
                    createdAt = cur.createdAt,
                    updatedAt = cur.updatedAt,
                    isDeleted = com.example.domain.vo.message.MessageIsDeleted.FALSE,
                    mentions = cur.mentions,
                    channelId = cur.channelId
                )
                messageRepository.sendMessage(rebuilt)
            } else {
                // 없던 메시지면 제거 시도 취소: 아무 것도 하지 않음
            }
        } catch (e: Exception) {
            Log.e(TAG, "revertOptimisticDelete 실패: $messageId", e)
        }
    }

    // ================================
    // 🔧 메시지 수정 유틸리티
    // ================================

    /**
     * 기존 payload JSON에서 content 필드만 교체한 새로운 payload 생성 (간단한 방식)
     */
    private suspend fun createUpdatedPayloadWithPreservedAttachments(
        messageId: DocumentId,
        newContent: String
    ): MessagePayload {
        return try {
            // 기존 메시지 조회
            val existingMessage = messageRepository.findById(messageId.value)

            if (existingMessage != null) {
                // 기존 payload JSON을 파싱해서 content만 교체
                val originalJson = existingMessage.payload.asJsonObject()
                val updatedJson = buildJsonObject {
                    // 기존 모든 필드 복사
                    originalJson.forEach { (key, value) ->
                        if (key == MessagePayload.KEY_CONTENT) {
                            put(key, JsonPrimitive(newContent))  // content만 새 값으로 교체
                        } else {
                            put(key, value)  // 나머지는 그대로 유지 (attachments 포함)
                        }
                    }
                    // content 키가 없었다면 추가
                    if (!originalJson.containsKey(MessagePayload.KEY_CONTENT)) {
                        put(MessagePayload.KEY_CONTENT, JsonPrimitive(newContent))
                    }
                }

                Log.d(TAG, "✏️ 메시지 수정: content만 교체, 기존 구조 보존")
                MessagePayload(updatedJson.toString())
            } else {
                // 메시지를 찾을 수 없으면 단순 텍스트로 fallback
                Log.w(TAG, "⚠️ 메시지 수정: 기존 메시지 찾을 수 없음, 단순 텍스트로 처리")
                MessagePayload.forText(newContent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 메시지 수정: payload 생성 실패, 단순 텍스트로 fallback", e)
            MessagePayload.forText(newContent)
        }
    }

    // ================================
    // 🔧 JSON 유틸리티
    // ================================
    // MessagePayload 유틸 사용으로 대체됨

    /**
     * 이미지 업로드 재시도 로직 포함
     */
    private suspend fun uploadImageWithRetry(
        compressedUri: Uri,
        uploadPath: String,
        currentRetryCount: Int
    ): CustomResult<String, Exception> {
        return try {
            fileUseCases.uploadFileUseCase(compressedUri, uploadPath)
        } catch (e: Exception) {
            if (currentRetryCount < 2 && isRetryableError(e)) {
                Log.d(TAG, "🔄 이미지 업로드 재시도: ${currentRetryCount + 1}/2")
                delay(1000L * (currentRetryCount + 1))
                uploadImageWithRetry(compressedUri, uploadPath, currentRetryCount + 1)
            } else {
                Log.e(TAG, "❌ 이미지 업로드 최종 실패", e)
                CustomResult.Failure(e)
            }
        }
    }


    /**
     * 메시지 재전송 기능
     */
    suspend fun retryFailedMessage(messageId: String): CustomResult<Unit, Exception> {
        Log.d(TAG, "🔄 실패한 메시지 재전송 시도: $messageId")

        return try {
            // Room DB에서 실패한 메시지 조회 (로컬 전용 findById)
            val message = messageRepository.findById(messageId)
            if (message != null) {
                val payload = message.payload

                // 이미지 메시지인지 확인
                val imageUrls = extractImageUrlsFromPayload(payload.value)

                if (imageUrls.isNotEmpty()) {
                    // 이미지 메시지 재전송 - 이미 업로드된 URL로 바로 전송
                    val retryMessageId = DocumentId(java.util.UUID.randomUUID().toString())
                    val result = sendMessageInternal(
                        messageId = retryMessageId,
                        senderId = message.senderId,
                        messageType = message.messageType,
                        payload = payload,
                        replyToMessageId = message.replyToMessageId
                    )

                    if (result is Success) {
                        // 기존 실패한 메시지 삭제
                        messageRepository.delete(DocumentId(messageId))
                        // OutBox 상태는 WebSocket ACK 처리에서 자동으로 DISPATCHED로 업데이트됩니다
                    }

                    when (result) {
                        is Success -> Success(Unit)
                        is CustomResult.Failure -> result
                        is CustomResult.Loading -> result
                        is CustomResult.Initial -> result
                        is CustomResult.Progress -> result
                    }
                } else {
                    // 일반 텍스트 메시지 재전송
                    val retryMessageId = DocumentId(java.util.UUID.randomUUID().toString())
                    val result = sendMessageInternal(
                        messageId = retryMessageId,
                        senderId = message.senderId,
                        messageType = message.messageType,
                        payload = payload,
                        replyToMessageId = message.replyToMessageId
                    )

                    if (result is Success) {
                        // 기존 실패한 메시지 삭제
                        messageRepository.delete(DocumentId(messageId))
                        // OutBox 상태는 WebSocket ACK 처리에서 자동으로 DISPATCHED로 업데이트됩니다
                    }

                    when (result) {
                        is Success -> Success(Unit)
                        is CustomResult.Failure -> result
                        is CustomResult.Progress -> result
                        is CustomResult.Initial -> result
                        is CustomResult.Loading -> result
                    }
                }
            } else {
                Log.e(TAG, "❌ 재전송할 메시지 조회 실패: $messageId")
                CustomResult.Failure(Exception("재전송할 메시지를 찾을 수 없습니다"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 메시지 재전송 중 예외", e)
            CustomResult.Failure(e)
        }
    }

    // ================================
    // 🎯 이미지 캐싱 및 성능 최적화
    // ================================


    /**
     * 메시지를 실패 상태로 표시
     */
    private suspend fun markMessageAsFailed(messageId: DocumentId) {
        try {
            // OutBox 상태는 handleMessageFailure에서 자동으로 FAILED 상태로 업데이트됩니다
            Log.e(TAG, "❌ 메시지 실패 상태로 표시: ${messageId.value}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 메시지 실패 표시 중 예외", e)
        }
    }

    /**
     * 실패한 이미지 메시지 재전송
     */
    suspend fun retryFailedImageMessage(messageId: DocumentId): CustomResult<Unit, Exception> {
        return try {
            Log.d(TAG, "🔄 실패한 이미지 메시지 재전송 시작: ${messageId.value}")

            val message = messageRepository.findById(messageId.value)
            when (message) {
                null -> CustomResult.Failure(Exception("재전송할 메시지를 찾을 수 없습니다"))
                else -> {
                    val payload = message.payload

                    // 업로드 중인 첨부파일이 있는지 확인
                    if (payload.hasUploadingAttachments()) {
                        Log.i(TAG, "📋 업로드 실패 메시지 재처리 시작")

                        // 로컬 URI들 추출
                        val attachments = payload.getAttachments()
                        val localUris = attachments
                            .filter { it["uploading"] as? Boolean == true }
                            .map { it["url"] as? String }
                            .filterNotNull()
                            .map { Uri.parse(it) }

                        if (localUris.isNotEmpty()) {
                            // 새로운 단순한 백그라운드 업로드 시작
                            val uriToPathMapping = mutableMapOf<String, String>()
                            localUris.forEachIndexed { index, uri ->
                                val ext = ImageCompressor.getExtension(context, uri) ?: "jpg"
                                val fileName = "${messageId.value}_retry_${index}.${ext}"
                                val storagePath = CollectionPath.storageMessageAttachment(
                                    roomId,
                                    messageId.value,
                                    fileName
                                ).value
                                uriToPathMapping[uri.toString()] = storagePath
                            }

                            startBackgroundImageUpload(
                                messageId = messageId,
                                imageUris = localUris,
                                uriToPathMapping = uriToPathMapping,
                                textContent = payload.getTextContent() ?: ""
                            )

                            Success(Unit)
                        } else {
                            CustomResult.Failure(Exception("재전송할 로컬 이미지를 찾을 수 없습니다"))
                        }
                    } else {
                        // 일반 메시지 재전송
                        val retryMessageId = DocumentId(java.util.UUID.randomUUID().toString())
                        val result = sendMessageInternal(
                            messageId = retryMessageId,
                            senderId = message.senderId,
                            messageType = message.messageType,
                            payload = payload,
                            replyToMessageId = message.replyToMessageId
                        )
                        when (result) {
                            is Success -> {
                                // OutBox 상태는 WebSocket ACK 처리에서 자동으로 DISPATCHED로 업데이트됩니다
                                Success(Unit)
                            }

                            is CustomResult.Failure -> result
                            else -> CustomResult.Failure(Exception("알 수 없는 전송 상태"))
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ 실패한 이미지 메시지 재전송 중 예외", e)
            CustomResult.Failure(e)
        }
    }

}
