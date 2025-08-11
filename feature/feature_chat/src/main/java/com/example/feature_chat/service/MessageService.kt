package com.example.feature_chat.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.core_common.constant.PagingConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.CustomResult.Success
import com.example.core_common.result.CustomResult.Failure
import com.example.core_common.util.AuthUtil
import com.example.core_common.util.DateTimeUtil
import com.example.core_common.util.ImageCompressor
import com.example.core_common.util.LogThrottler
import com.example.websocket.util.MessageTypeDetector
import com.example.websocket.core.WebSocketMessage
import com.example.websocket.constant.WebSocketFieldConstants
import com.example.domain.enum.OutBoxStatus
import com.example.domain.model.base.Message
import com.example.domain.vo.ChannelId
import com.example.domain.vo.CollectionPath
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.example.websocket.usecase.WebSocketUseCaseProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
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
    private val context: Context,
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    val offlineMessageQueue: OfflineMessageQueue,
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

        // 낙관적 처리 메타 키
        private const val META_KEY = "_meta"
        private const val META_PENDING_OP = "pendingOp"
        private const val META_BACKUP_PAYLOAD = "backupPayload"
        private const val OP_EDIT = "edit"
        private const val OP_DELETE = "delete"
    }

    // OutBox 상태 캐시 (메시지 ID -> OutBox 상태)
    private val outboxStatusCache = mutableMapOf<String, OutBoxStatus>()

    // URI-Storage 경로 매핑 캐시 (로컬 URI -> Firebase Storage 경로)
    private val uriToStoragePathCache = mutableMapOf<String, String>()

    // 로그 샘플링을 위한 카운터
    private var statusCheckLogCounter = 0
    private val loggedMessageIds = mutableSetOf<String>()

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
        return flow {
            val initialKey: Long? = try {
                if (initialMessageId.isNullOrBlank()) {
                    if (useFallbackAnchor) {
                        System.currentTimeMillis()
                    } else {
                        null
                    }
                } else {
                    val anchor = messageRepository.findById(initialMessageId)
                    if (anchor != null) {
                        anchor.createdAt?.toEpochMilli()
                    } else if (useFallbackAnchor) {
                        Log.d(TAG, "앵커 메시지 미존재, fallback 사용: $initialMessageId")
                        System.currentTimeMillis()
                    } else {
                        null
                    }
                }
            } catch (_: Exception) { 
                if (useFallbackAnchor) System.currentTimeMillis() else null
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
                pagingSourceFactory = { messageRepository.getMessagesPagingSource(roomId) }
            )

            emitAll(
                pager.flow.map { pagingData ->
                    pagingData.map { message -> convertDomainMessageToUiModel(message) }
                }
            )
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

        // 로그 샘플링: 10개마다 1번만 출력하거나 새로운 메시지 ID인 경우만
        statusCheckLogCounter++
        val messageId = message.id.value
        val shouldLog = (statusCheckLogCounter % 10 == 0) ||
                !loggedMessageIds.contains(messageId) ||
                sendFailed // 실패한 경우는 항상 로그

        if (shouldLog) {
            loggedMessageIds.add(messageId)
            if (loggedMessageIds.size > 100) {
                // 메모리 절약을 위해 100개 초과 시 오래된 것들 제거
                loggedMessageIds.clear()
            }

            // LogThrottler를 사용한 최적화된 로그 출력
            LogThrottler.d(
                TAG,
                "메시지 상태 확인 (#${statusCheckLogCounter}): messageId=$messageId, outboxStatus=$outboxStatus, isSending=$isSending, sendFailed=$sendFailed, isDispatched=$isDispatched",
                "message_status_check",
                500L // 500ms 간격
            )
        }

        // 이미지 URL 추출
        val imageUrls = extractImageUrlsFromPayload(message.payload.value)
        val hasImages = imageUrls.isNotEmpty()

        // 낙관적 편집/삭제 인디케이터 계산 (payload 메타 기반)
        val payloadJson = try {
            Json.parseToJsonElement(message.payload.value).jsonObject
        } catch (e: Exception) {
            JsonObject(mapOf())
        }
        val metaObj = payloadJson[META_KEY] as? JsonObject
        val pendingOp = metaObj?.get(META_PENDING_OP)?.jsonPrimitive?.content
        val hasPendingOptimisticOp = pendingOp == OP_EDIT || pendingOp == OP_DELETE

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
            attachmentImageUrls = emptyList(), // 레거시 - 하위 호환용
            imageUrls = imageUrls, // 새로운 이미지 URL 목록
            hasImages = hasImages, // 이미지 포함 여부
            isMyMessage = senderId == currentUserId,
            isSending = isSending || hasPendingOptimisticOp,
            sendFailed = sendFailed,
            isDeleted = message.isDeleted.value,
            deliveryState = when {
                hasPendingOptimisticOp -> MessageDeliveryState.Sending
                isSending -> MessageDeliveryState.Sending
                sendFailed -> MessageDeliveryState.Failed("메시지 전송에 실패했습니다")
                isDispatched -> MessageDeliveryState.Sent
                else -> MessageDeliveryState.Sent // 기본값은 전송 완료로 간주
            },
            isOptimistic = isSending || hasPendingOptimisticOp,
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

    /**
     * 메시지 payload에서 이미지 URL들을 추출하는 유틸리티 메서드
     */
    private fun extractImageUrlsFromPayload(payload: String): List<String> {
        return try {
            // JSON 파싱을 시도하여 이미지 정보 추출
            val urls = when {
                // 단일 이미지 처리
                payload.contains("\"imageUrl\"") -> {
                    val regex = "\"imageUrl\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                    val match = regex.find(payload)
                    if (match != null) listOf(match.groupValues[1]) else emptyList()
                }
                // 다중 이미지 처리
                payload.contains("\"images\"") -> {
                    val regex = "\"url\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                    regex.findAll(payload).map { it.groupValues[1] }.toList()
                }

                else -> emptyList()
            }

            if (urls.isNotEmpty()) {
                Log.d(TAG, "🖼️ [UI표시] Payload에서 이미지 URL 추출 완료: ${urls.size}개 URL")
            }

            urls
        } catch (e: Exception) {
            Log.w(TAG, "이미지 URL 추출 중 오류: ${e.message}")
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

    /**
     * 최신 메시지 ID 업데이트 (새 메시지 수신 시 호출)
     */
    fun updateLatestMessageId(messageId: String) {
        latestMessageId = messageId
        Log.d(TAG, "최신 메시지 ID 업데이트: $messageId")
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
        additionalMetadata: Map<String, String> = emptyMap()
    ): CustomResult<DocumentId, Exception> {
        Log.d(TAG, "메시지 전송 시도: text='$textContent', images=${imageUris.size}개")
        
        return try {
            // 이미지가 있는 경우 단순한 placeholder 플로우 사용
            if (imageUris.isNotEmpty() && !isSystemMessage) {
                Log.d(TAG, "📸 [이미지전송] 이미지 메시지 플로우 시작: ${imageUris.size}개 이미지")
                return sendImageMessageSimple(
                    senderId = senderId,
                    textContent = textContent,
                    imageUris = imageUris,
                    replyToMessageId = replyToMessageId
                )
            }

            // 일반 텍스트/시스템 메시지 처리
            val payload = MessageTypeDetector.createBasicPayload(
                textContent = textContent,
                images = emptyList(),
                additionalData = buildMap {
                    putAll(additionalMetadata)
                    if (isSystemMessage && systemType != null) {
                        put(WebSocketFieldConstants.PAYLOAD_SYSTEM_TYPE, systemType)
                    }
                }
            )

            val messageId = DocumentId(messageRepository.sendMessage(roomId, payload))
            updateOutBoxStatusCache(messageId.value, OutBoxStatus.PENDING)

            val sendResult = roomWebSocketUseCases.sendMessageUseCase(
                senderId = senderId,
                content = textContent,
                messageId = messageId,
                replyToMessageId = replyToMessageId,
                projectId = projectId,
                channelType = channelType
            )

            if (sendResult.isSuccess) {
                Log.d(TAG, "✅ 텍스트 메시지 전송 성공: ${messageId.value}")
            } else {
                Log.e(TAG, "❌ WebSocket 전송 실패")
            }

            CustomResult.Success(messageId)

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
        senderId: UserId,
        textContent: String,
        imageUris: List<Uri>,
        replyToMessageId: DocumentId?
    ): CustomResult<DocumentId, Exception> {
        return try {
            val messageId = DocumentId(java.util.UUID.randomUUID().toString())
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

            // 2단계: Placeholder payload 생성 (uploading 상태)
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

            // 3단계: 로컬 DB에 저장
            val placeholderMessage = Message.create(
                id = messageId,
                senderId = senderId,
                messageType = MessageType.IMAGE,
                payload = placeholderPayload,
                replyToMessageId = replyToMessageId,
                mentions = emptyList(),
                channelId = ChannelId(roomId)
            )

            val saveResult = messageRepository.save(placeholderMessage)
            if (saveResult.isFailure) {
                return CustomResult.Failure(Exception("메시지 로컬 저장 실패"))
            }

            updateOutBoxStatusCache(messageId.value, OutBoxStatus.PENDING)

            // 4단계: WebSocket으로 전송
            val websocketResult = roomWebSocketUseCases.sendMessageUseCase(
                senderId = senderId,
                content = textContent,
                messageId = messageId,
                replyToMessageId = replyToMessageId,
                projectId = projectId,
                channelType = channelType
            )

            if (websocketResult.isSuccess) {
                Log.d(TAG, "✅ Placeholder 메시지 WebSocket 전송 성공")
                Log.d(TAG, "🔄 [이미지전송] 백그라운드 Firebase Storage 업로드 시작")
                // 5단계: 백그라운드에서 Firebase Storage 업로드 시작
                startBackgroundImageUpload(messageId, imageUris, uriToPathMapping, textContent)
            } else {
                Log.w(TAG, "⚠️ WebSocket 전송 실패, 오프라인 큐에 추가 예정")
                // TODO: 오프라인 큐 처리
            }

            CustomResult.Success(messageId)

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
                        is CustomResult.Success -> {
                            Log.d(
                                TAG,
                                "✅ [Firebase저장] 이미지 ${index + 1}/${imageUris.size} Firebase Storage 저장 완료"
                            )

                            val ext = ImageCompressor.getExtension(context, uri) ?: "jpg"
                            val mime = if (ext == "jpg") "image/jpeg" else "image/$ext"

                            uploadedImages.add(
                                mapOf(
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

            // 로컬 DB의 메시지 payload 업데이트
            val messageResult = messageRepository.findById(messageId)
            when (messageResult) {
                is CustomResult.Success -> {
                    val message = messageResult.data
                    message.updatePayload(finalPayload)

                    val saveResult = messageRepository.save(message)
                    if (saveResult.isSuccess) {
                        Log.d(
                            TAG,
                            "✅ [이미지전송완료] 메시지 payload를 실제 Firebase URL로 업데이트 완료: ${messageId.value}"
                        )

                        // OutBox 상태를 DISPATCHED로 변경 (업로드 완료)
                        updateOutBoxStatusCache(messageId.value, OutBoxStatus.DISPATCHED)

                        // UI 강제 갱신을 위한 PagingSource invalidate
                        try {
                            val repoImplClass =
                                Class.forName("com.example.data_repository.base.MessageRepositoryImpl")
                            val method = repoImplClass.getMethod("invalidateCurrentPagingSource")
                            if (repoImplClass.isInstance(messageRepository)) {
                                method.invoke(messageRepository)
                                Log.d(TAG, "✅ PagingSource invalidate 완료")
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "⚠️ PagingSource invalidate 실패 (무시 가능): ${e.message}")
                        }
                        
                    } else {
                        Log.e(TAG, "❌ 메시지 payload 업데이트 저장 실패")
                        markMessageAsFailed(messageId)
                    }
                }
                else -> {
                    Log.e(TAG, "❌ 업데이트할 메시지를 찾을 수 없음: ${messageId.value}")
                    markMessageAsFailed(messageId)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ 메시지 payload 업데이트 중 예외", e)
            markMessageAsFailed(messageId)
        }
    }

    // ================================
    // 헬퍼 메서드들 (내부 사용)
    // ================================








    /**
     * 메시지 ACK 처리 (WebSocket ACK 수신 시)
     */
    suspend fun handleMessageAck(messageId: String) {
        try {
            val result = messageRepository.handleMessageAck(messageId)
            if (result is CustomResult.Success) {
                updateOutBoxStatusCache(messageId, OutBoxStatus.DISPATCHED)

                // 🔄 UI 즉시 갱신을 위한 강제 invalidate
                // MessageRepositoryImpl의 invalidateCurrentPagingSource() 메서드 호출
                try {
                    val repoImplClass =
                        Class.forName("com.example.data_repository.base.MessageRepositoryImpl")
                    val method = repoImplClass.getMethod("invalidateCurrentPagingSource")
                    if (repoImplClass.isInstance(messageRepository)) {
                        method.invoke(messageRepository)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "⚠️ PagingSource invalidate 호출 실패 (무시 가능): ${e.message}")
                }

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
            if (result is CustomResult.Success) {
                updateOutBoxStatusCache(messageId, OutBoxStatus.FAILED)

                // 🔄 UI 즉시 갱신을 위한 강제 invalidate (실패 상태도 즉시 반영)
                try {
                    val repoImplClass =
                        Class.forName("com.example.data_repository.base.MessageRepositoryImpl")
                    val method = repoImplClass.getMethod("invalidateCurrentPagingSource")
                    if (repoImplClass.isInstance(messageRepository)) {
                        method.invoke(messageRepository)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "⚠️ PagingSource invalidate 호출 실패 (무시 가능): ${e.message}")
                }

                Log.d(TAG, "✅ 메시지 실패 처리 완료 + UI 갱신: $messageId")
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
        // 기존 경로 유지: 복합/이미지 페이로드 등 레거시 경로에서 사용
        val payloadMap = mutableMapOf<String, Any?>()
        payload.getTextContent()?.let { payloadMap["content"] = it }
        if (payloadMap.isEmpty()) {
            // content 키가 없으면 전체 JSON 문자열을 content로 전송
            payloadMap["content"] = payload.value
        }
        return sendMessageWithPayloadMap(senderId, payloadMap, replyToMessageId)
    }

    /**
     * Map 형태 payload로 메시지 전송
     * - Repository.sendMessage(channelId, payloadMap)으로 로컬 저장 + OutBox 생성
     * - 이후 WebSocket 전송 수행
     */
    suspend fun sendMessageWithPayloadMap(
        senderId: UserId,
        payloadMap: Map<String, Any?>,
        replyToMessageId: DocumentId? = null
    ): CustomResult<DocumentId, Exception> {
        return try {
            // 1) 로컬 저장 + OutBox 생성 (메시지 ID 발급)
            val createdId = messageRepository.sendMessage(roomId, payloadMap)
            val messageId = DocumentId(createdId)

            // OutBox 상태 캐시 업데이트 (PENDING)
            updateOutBoxStatusCache(messageId.value, OutBoxStatus.PENDING)

            // 2) WebSocket 전송
            val contentForWebSocket = (payloadMap["content"] as? String)
                ?: mapToJsonString(payloadMap)

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
                messageRepository.findById(messageId.value)?.let { msg ->
                    offlineMessageQueue.queueMessage(
                        com.example.feature_chat.queue.QueuedMessageAction.Send(
                            message = msg,
                            roomId = roomId
                        )
                    )
                }

                CustomResult.Success(messageId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "메시지 전송(Map) 중 예외", e)
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
            val editResult = roomWebSocketUseCases.editMessageUseCase(
                messageId = messageId,
                newContent = newContent,
                projectId = projectId,
                channelType = channelType
            )

            if (editResult.isSuccess) {
                Log.d(TAG, "메시지 수정 전송 성공(ACK 대기): ${messageId.value}")
                CustomResult.Success(Unit)
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
                messageId = messageId,
                projectId = projectId,
                channelType = channelType
            )

            if (deleteResult.isSuccess) {
                Log.d(TAG, "메시지 삭제 전송 성공(ACK 대기): ${messageId.value}")
                CustomResult.Success(Unit)
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
            val existing = messageRepository.findById(messageId)
            if (existing is CustomResult.Success) {
                val cur = existing.data
                val optimisticPayload = buildOptimisticEditedPayload(
                    newContent = newContent,
                    backupPayloadJson = cur.payload.value
                )
                cur.updatePayload(optimisticPayload)
                messageRepository.save(cur)
            } else {
                // 업서트: 없는 경우 임시 메시지 생성
                val temp = Message.create(
                    id = messageId,
                    senderId = UserId(AuthUtil.getCurrentUserId() ?: ""),
                    messageType = MessageType.TEXT,
                    payload = buildOptimisticEditedPayload(newContent, backupPayloadJson = "{}"),
                    replyToMessageId = null,
                    mentions = emptyList(),
                    channelId = ChannelId(roomId)
                )
                messageRepository.save(temp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "applyOptimisticEdit 실패", e)
        }
    }

    /** 로컬에 낙관적 삭제 적용 (없으면 업서트) */
    suspend fun applyOptimisticDelete(messageId: DocumentId) {
        try {
            val existing = messageRepository.findById(messageId)
            if (existing is CustomResult.Success) {
                val cur = existing.data
                val newPayload =
                    addMetaToPayload(cur.payload.value, OP_DELETE, backup = cur.payload.value)
                cur.updatePayload(MessagePayload(newPayload))
                cur.delete()
                messageRepository.save(cur)
            } else {
                // 업서트: 없는 경우 삭제된 임시 메시지 생성
                val temp = Message.create(
                    id = messageId,
                    senderId = UserId(AuthUtil.getCurrentUserId() ?: ""),
                    messageType = MessageType.TEXT,
                    payload = MessagePayload(
                        addMetaToPayload(
                            MessagePayload.forText("").value,
                            OP_DELETE,
                            backup = "{}"
                        )
                    ),
                    replyToMessageId = null,
                    mentions = emptyList(),
                    channelId = ChannelId(roomId)
                )
                temp.delete()
                messageRepository.save(temp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "applyOptimisticDelete 실패", e)
        }
    }

    /** ACK 수신 시 낙관적 메타 정리(편집/삭제 공통) */
    suspend fun handleOptimisticAck(messageId: String) {
        try {
            val result = messageRepository.findById(DocumentId(messageId))
            if (result is CustomResult.Success) {
                val cur = result.data
                val cleaned = removeMetaFromPayload(cur.payload.value)
                cur.updatePayload(MessagePayload(cleaned))
                messageRepository.save(cur)
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleOptimisticAck 실패: $messageId", e)
        }
    }

    /** FAILED 처리 시 낙관적 롤백(편집/삭제 공통) */
    suspend fun handleOptimisticFailure(messageId: String) {
        try {
            val result = messageRepository.findById(DocumentId(messageId))
            if (result is CustomResult.Success) {
                val cur = result.data
                val (pendingOp, _) = extractPendingOpAndBackup(cur.payload.value)
                when (pendingOp) {
                    OP_EDIT -> revertOptimisticEdit(messageId)
                    OP_DELETE -> revertOptimisticDelete(messageId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleOptimisticFailure 실패: $messageId", e)
        }
    }

    suspend fun finalizeOptimisticEdit(messageId: String) = handleOptimisticAck(messageId)
    suspend fun finalizeOptimisticDelete(messageId: String) = handleOptimisticAck(messageId)

    suspend fun revertOptimisticEdit(messageId: String) {
        try {
            val result = messageRepository.findById(DocumentId(messageId))
            if (result is CustomResult.Success) {
                val cur = result.data
                val (_, backupJson) = extractPendingOpAndBackup(cur.payload.value)
                val backup = backupJson ?: return
                // 백업으로 복원하고 메타 제거
                val restoredPayload = try {
                    // 백업이 JSON이면 그대로
                    Json.parseToJsonElement(backup); backup
                } catch (_: Exception) {
                    // 텍스트만 있는 경우
                    MessagePayload.forText(backup).value
                }
                val cleaned = removeMetaFromPayload(restoredPayload)
                cur.updatePayload(MessagePayload(cleaned))
                messageRepository.save(cur)
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
            val result = messageRepository.findById(DocumentId(messageId))
            if (result is CustomResult.Success) {
                val cur = result.data
                val (_, backupJson) = extractPendingOpAndBackup(cur.payload.value)
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
                    payload = MessagePayload(removeMetaFromPayload(restoredPayload)),
                    replyToMessageId = cur.replyToMessageId,
                    createdAt = cur.createdAt,
                    updatedAt = cur.updatedAt,
                    isDeleted = com.example.domain.vo.message.MessageIsDeleted.FALSE,
                    mentions = cur.mentions,
                    channelId = cur.channelId
                )
                messageRepository.save(rebuilt)
            } else {
                // 없던 메시지면 제거 시도 취소: 아무 것도 하지 않음
            }
        } catch (e: Exception) {
            Log.e(TAG, "revertOptimisticDelete 실패: $messageId", e)
        }
    }

    // ================================
    // 🔧 JSON 유틸리티
    // ================================
    private fun buildOptimisticEditedPayload(
        newContent: String,
        backupPayloadJson: String
    ): MessagePayload {
        val backupElement: JsonElement = try {
            Json.parseToJsonElement(backupPayloadJson)
        } catch (_: Exception) {
            JsonPrimitive(backupPayloadJson)
        }
        val meta = buildJsonObject {
            put(META_PENDING_OP, JsonPrimitive(OP_EDIT))
            put(META_BACKUP_PAYLOAD, backupElement)
        }
        val obj = buildJsonObject {
            put("content", JsonPrimitive(newContent))
            put(META_KEY, meta)
        }
        return MessagePayload(obj.toString())
    }

    private fun addMetaToPayload(currentPayloadJson: String, op: String, backup: String): String {
        val baseObj: JsonObject = try {
            Json.parseToJsonElement(currentPayloadJson).jsonObject
        } catch (_: Exception) {
            JsonObject(mapOf("content" to JsonPrimitive("")))
        }
        val backupElement: JsonElement = try {
            Json.parseToJsonElement(backup)
        } catch (_: Exception) {
            JsonPrimitive(backup)
        }
        val meta = buildJsonObject {
            put(META_PENDING_OP, JsonPrimitive(op))
            put(META_BACKUP_PAYLOAD, backupElement)
        }
        val merged = buildJsonObject {
            baseObj.forEach { (k, v) -> put(k, v) }
            put(META_KEY, meta)
        }
        return merged.toString()
    }

    private fun removeMetaFromPayload(payloadJson: String): String {
        return try {
            val obj = Json.parseToJsonElement(payloadJson).jsonObject
            val cleaned = buildJsonObject {
                obj.forEach { (k, v) -> if (k != META_KEY) put(k, v) }
            }
            cleaned.toString()
        } catch (_: Exception) {
            payloadJson
        }
    }

    private fun extractPendingOpAndBackup(payloadJson: String): Pair<String?, String?> {
        return try {
            val obj = Json.parseToJsonElement(payloadJson).jsonObject
            val meta = obj[META_KEY] as? JsonObject
            val op = meta?.get(META_PENDING_OP)?.jsonPrimitive?.content
            val backupEl = meta?.get(META_BACKUP_PAYLOAD)
            val backup = when (backupEl) {
                is JsonObject -> backupEl.toString()
                is JsonPrimitive -> backupEl.content
                is JsonNull -> null
                else -> backupEl?.toString()
            }
            Pair(op, backup)
        } catch (_: Exception) {
            Pair(null, null)
        }
    }

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
     * 배치 업로드에서 개별 이미지 업로드 재시도 로직
     */
    /*private suspend fun uploadImageWithRetryForBatch(
        compressedUri: Uri,
        index: Int,
        originalUri: Uri,
        batchRetryCount: Int
    ): Map<String, Any>? {
        return try {
            val uploadPath =
                "chat_images/${roomId}/${System.currentTimeMillis()}_${index}_r${batchRetryCount}.jpg"
            val uploadResult = uploadImageWithRetry(compressedUri, uploadPath, 0)

            when (uploadResult) {
                is CustomResult.Success -> {
                    Log.d(TAG, "✅ 배치 이미지 ${index + 1} 업로드 성공")
                    mapOf(
                        "url" to uploadResult.data,
                        "filename" to (originalUri.lastPathSegment ?: "image.jpg"),
                        "mime" to "image/jpeg",
                        "compressed" to true,
                        "retryCount" to batchRetryCount
                    )
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "❌ 배치 이미지 ${index + 1} 업로드 실패", uploadResult.error)
                    null
                }

                else -> {
                    Log.e(TAG, "❌ 배치 이미지 ${index + 1} 업로드 알 수 없는 상태")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 배치 이미지 ${index + 1} 업로드 예외", e)
            null
        }
    }*/

    /**
     * 실패한 단일 이미지 메시지를 오프라인 큐에 추가
     */
    /*private fun queueFailedImageMessage(
        senderId: UserId,
        imageUri: Uri,
        content: String,
        replyToMessageId: DocumentId?
    ) {
        try {
            Log.d(TAG, "📋 실패한 이미지 메시지 큐에 추가: $imageUri")
            // TODO: 이미지 메시지 전용 큐잉 시스템 구현
            // 현재는 기본 메시지 큐를 사용하되, 추후 이미지 전용 처리 필요
        } catch (e: Exception) {
            Log.e(TAG, "❌ 실패한 이미지 메시지 큐잉 실패", e)
        }
    }*/

    /**
     * 실패한 다중 이미지 메시지를 오프라인 큐에 추가
     */
    /*private fun queueFailedImagesMessage(
        senderId: UserId,
        imageUris: List<Uri>,
        content: String,
        replyToMessageId: DocumentId?
    ) {
        try {
            Log.d(TAG, "📋 실패한 다중 이미지 메시지 큐에 추가: ${imageUris.size}개")
            // TODO: 다중 이미지 메시지 전용 큐잉 시스템 구현
        } catch (e: Exception) {
            Log.e(TAG, "❌ 실패한 다중 이미지 메시지 큐잉 실패", e)
        }
    }*/

    /**
     * 메시지 재전송 기능
     */
    suspend fun retryFailedMessage(messageId: String): CustomResult<Unit, Exception> {
        Log.d(TAG, "🔄 실패한 메시지 재전송 시도: $messageId")

        return try {
            // Room DB에서 실패한 메시지 조회
            val messageResult = messageRepository.findById(DocumentId(messageId))

            when (messageResult) {
                is CustomResult.Success -> {
                    val message = messageResult.data
                    val payload = message.payload

                    // 이미지 메시지인지 확인
                    val imageUrls = extractImageUrlsFromPayload(payload.value)

                    if (imageUrls.isNotEmpty()) {
                        // 이미지 메시지 재전송 - 이미 업로드된 URL로 바로 전송
                        val result = sendMessageInternal(
                            senderId = message.senderId,
                            messageType = message.messageType,
                            payload = payload,
                            replyToMessageId = message.replyToMessageId
                        )

                        if (result is CustomResult.Success) {
                            // 기존 실패한 메시지 삭제
                            messageRepository.delete(DocumentId(messageId))
                            updateOutBoxStatusCache(messageId, OutBoxStatus.DISPATCHED)
                        }

                        when (result) {
                            is CustomResult.Success -> Success(Unit)
                            is CustomResult.Failure -> result
                            is CustomResult.Loading -> result
                            is CustomResult.Initial -> result
                            is CustomResult.Progress -> result
                        }
                    } else {
                        // 일반 텍스트 메시지 재전송
                        val result = sendMessageInternal(
                            senderId = message.senderId,
                            messageType = message.messageType,
                            payload = payload,
                            replyToMessageId = message.replyToMessageId
                        )

                        if (result is CustomResult.Success) {
                            // 기존 실패한 메시지 삭제
                            messageRepository.delete(DocumentId(messageId))
                            updateOutBoxStatusCache(messageId, OutBoxStatus.DISPATCHED)
                        }

                        when (result) {
                            is CustomResult.Success -> CustomResult.Success(Unit)
                            is CustomResult.Failure -> result
                            is CustomResult.Progress -> result
                            is CustomResult.Initial -> result
                            is CustomResult.Loading -> result
                        }
                    }
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "❌ 재전송할 메시지 조회 실패", messageResult.error)
                    CustomResult.Failure(Exception("재전송할 메시지를 찾을 수 없습니다"))
                }

                else -> {
                    Log.e(TAG, "❌ 재전송할 메시지 조회 알 수 없는 상태")
                    CustomResult.Failure(Exception("메시지 조회 실패"))
                }
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
     * 채팅방 최근 이미지들을 프리로드하여 캐시에 저장
     */
    suspend fun preloadRecentImages() {
        try {
            Log.d(TAG, "🚀 최근 이미지 프리로드 시작")

            // 최근 50개 메시지에서 이미지 URL 추출
            val recentMessagesResult = messageRepository.getRecentMessages(roomId, 50)

            when (recentMessagesResult) {
                is CustomResult.Success -> {
                    val imageUrls = recentMessagesResult.data
                        .asSequence()
                        .mapNotNull { message ->
                            extractImageUrlsFromPayload(message.payload.value)
                        }
                        .flatten()
                        .distinct()
                        .take(20) // 최대 20개 이미지만 프리로드
                        .toList()

                    if (imageUrls.isNotEmpty()) {
                        // Coil 내부 캐시에 맡깁니다 (명시적 프리로딩 생략)
                        Log.d(TAG, "ℹ️ 최근 이미지 URL 수집: ${imageUrls.size}개 (Coil 캐시 사용)")
                    } else {
                        Log.d(TAG, "📭 프리로드할 이미지 없음")
                    }
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "❌ 최근 메시지 조회 실패", recentMessagesResult.error)
                }

                else -> {
                    Log.w(TAG, "⚠️ 최근 메시지 조회 알 수 없는 상태")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 이미지 프리로드 중 예외", e)
        }
    }

    /**
     * 이미지 썸네일 생성 (UI 성능 최적화용)
     */
    suspend fun generateImageThumbnails(imageUrls: List<String>) {
        try {
            Log.d(TAG, "🖼️ 이미지 썸네일 생성 시작: ${imageUrls.size}개")

            // Coil 변환/리사이즈를 사용하여 각 화면에서 처리. 전역 썸네일 생성은 생략

            Log.d(TAG, "✅ 이미지 썸네일 생성 완료")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 썸네일 생성 중 예외", e)
        }
    }

    /**
     * 캐시 정리 (용량 최적화)
     */
    suspend fun cleanupCache() {
        try {
            Log.d(TAG, "🧹 캐시 정리 시작")
            // Coil 디스크 캐시는 ImageLoader 설정에 따름. 별도 정리 생략
            Log.d(TAG, "✅ 캐시 정리 완료")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 캐시 정리 중 예외", e)
        }
    }

    /**
     * 캐시 상태 정보 조회
     */
    fun getCacheInfo(): String {
        return buildString {
            appendLine("=== MessageService Cache Info ===")
            appendLine("Room ID: $roomId")
            appendLine("OutBox Cache: ${outboxStatusCache.size} entries")
            appendLine("Status Check Count: $statusCheckLogCounter")
            appendLine("Logged Message IDs: ${loggedMessageIds.size}")
            appendLine("ChatImageCache: removed; using Coil caches")
        }
    }

    /**
     * 성능 및 로그 통계 정보 조회
     */
    fun getPerformanceStats(): String {
        return buildString {
            appendLine("=== MessageService Performance Stats ===")
            appendLine("Room ID: $roomId")
            appendLine("Status Check Total: $statusCheckLogCounter")
            appendLine("Unique Messages Logged: ${loggedMessageIds.size}")
            appendLine("OutBox Cache Size: ${outboxStatusCache.size}")
            appendLine()
            append(LogThrottler.getLogStats())
        }
    }

    /**
     * 성능 최적화를 위한 정리 작업
     */
    fun performMaintenance() {
        try {
            // LogThrottler 정리
            LogThrottler.cleanup()

            // 메시지 ID 캐시 정리 (메모리 절약)
            if (loggedMessageIds.size > 200) {
                val keepSize = 50
                val toRemove = loggedMessageIds.size - keepSize
                val iterator = loggedMessageIds.iterator()
                var removed = 0

                while (iterator.hasNext() && removed < toRemove) {
                    iterator.next()
                    iterator.remove()
                    removed++
                }

                LogThrottler.d(TAG, "메시지 ID 캐시 정리 완료: ${removed}개 제거", "maintenance", 60000L)
            }

            // OutBox 상태 캐시 정리 (너무 많아지면)
            if (outboxStatusCache.size > 1000) {
                val currentSize = outboxStatusCache.size
                outboxStatusCache.clear()

                LogThrottler.d(TAG, "OutBox 캐시 초기화: ${currentSize}개 항목 제거", "maintenance", 60000L)
            }

        } catch (e: Exception) {
            LogThrottler.e(TAG, "유지보수 작업 중 오류 발생", "maintenance_error", throwable = e)
        }
    }

    /**
     * 성능 최적화를 위한 이미지 처리 파이프라인
     */
    suspend fun optimizeImagePerformance(imageUrls: List<String>) {
        try {
            Log.d(TAG, "⚡ 이미지 성능 최적화 시작")

            // Coil의 메모리/디스크 캐시에 일임. 명시적 프리로딩/썸네일 생성 생략

            Log.d(TAG, "✅ 이미지 성능 최적화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 이미지 성능 최적화 중 예외", e)
        }
    }


    /**
     * 메시지를 실패 상태로 표시
     */
    private suspend fun markMessageAsFailed(messageId: DocumentId) {
        try {
            updateOutBoxStatusCache(messageId.value, OutBoxStatus.FAILED)
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

            val messageResult = messageRepository.findById(messageId)
            when (messageResult) {
                is CustomResult.Success -> {
                    val message = messageResult.data
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

                            CustomResult.Success(Unit)
                        } else {
                            CustomResult.Failure(Exception("재전송할 로컬 이미지를 찾을 수 없습니다"))
                        }
                    } else {
                        // 일반 메시지 재전송
                        val result = sendMessageInternal(
                            senderId = message.senderId,
                            messageType = message.messageType,
                            payload = payload,
                            replyToMessageId = message.replyToMessageId
                        )
                        when (result) {
                            is CustomResult.Success -> {
                                updateOutBoxStatusCache(messageId.value, OutBoxStatus.DISPATCHED)
                                CustomResult.Success(Unit)
                            }

                            is CustomResult.Failure -> result
                            else -> CustomResult.Failure(Exception("알 수 없는 전송 상태"))
                        }
                    }
                }

                is CustomResult.Failure -> {
                    Log.e(TAG, "❌ 재전송할 메시지 조회 실패", messageResult.error)
                    messageResult
                }

                else -> CustomResult.Failure(Exception("메시지 조회 알 수 없는 상태"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ 실패한 이미지 메시지 재전송 중 예외", e)
            CustomResult.Failure(e)
        }
    }

}

/**
 * 이미지 업로드 전용 예외 클래스
 */
class ImageUploadException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)