package com.example.feature_chat.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.core_common.cache.ChatImageCache
import com.example.core_common.result.CustomResult
import com.example.core_common.result.CustomResult.Success
import com.example.core_common.util.AuthUtil
import com.example.core_common.util.DateTimeUtil
import com.example.core_common.util.ImageCompressor
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
    private val context: Context,
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    val offlineMessageQueue: OfflineMessageQueue,
    private val messageRepository: MessageRepository,
    private val userProfileService: UserProfileService,
    private val fileUseCases: FileManagementUseCases,
    private val dmUseCaseProvider: DMUseCaseProvider,
    private val chatImageCache: ChatImageCache,
    private val roomId: String,
    private val projectId: String? = null,
    private val channelType: String = "chat"
) {

    companion object {
        private const val TAG = "MessageService"
        private const val PAGE_SIZE = 15 // 15개씩 로딩
        private const val INITIAL_LOAD_SIZE = PAGE_SIZE * 3 // 초기 3페이지 로드로 양방향 스크롤 여유 확보
        private const val MAX_SIZE = PAGE_SIZE * 10 // 드랍으로 인한 재로딩 방지
        private const val PREFETCH_DISTANCE = 10 // 경계 근접 전에 로드 트리거 강화
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

        // 이미지 URL 추출
        val imageUrls = extractImageUrlsFromPayload(message.payload.value)
        val hasImages = imageUrls.isNotEmpty()
        
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

    /**
     * 메시지 payload에서 이미지 URL들을 추출하는 유틸리티 메서드
     */
    private fun extractImageUrlsFromPayload(payload: String): List<String> {
        return try {
            // JSON 파싱을 시도하여 이미지 정보 추출
            when {
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
     * 이미지 메시지 전송 (단일 이미지) - 압축 기능 및 에러 핸들링 포함
     */
    suspend fun sendImageMessage(
        senderId: UserId,
        imageUri: Uri,
        content: String = "",
        replyToMessageId: DocumentId? = null,
        retryCount: Int = 0
    ): CustomResult<DocumentId, Exception> {
        Log.d(
            TAG,
            "이미지 메시지 전송 시도 (재시도: $retryCount): senderId=${senderId.value}, imageUri=$imageUri, roomId=$roomId"
        )
        
        return try {
            // 1단계: 이미지 압축 (재시도 시에는 캐시된 압축 결과 사용 가능)
            Log.d(TAG, "🔄 이미지 압축 시작...")
            val compressionOptions = ImageCompressor.CompressionOptions(
                maxWidth = 1920,
                maxHeight = 1920,
                quality = if (retryCount > 0) 75 else 85, // 재시도 시 품질 낮춤
                maxFileSizeBytes = if (retryCount > 0) 2 * 1024 * 1024 else 3 * 1024 * 1024 // 재시도 시 파일 크기 제한 강화
            )

            val compressedUri = ImageCompressor.compressImage(context, imageUri, compressionOptions)
            Log.d(TAG, "✅ 이미지 압축 완료: $compressedUri")

            // 2단계: Firebase Storage 업로드 (재시도 로직 포함)
            val uploadPath =
                "chat_images/${roomId}/${System.currentTimeMillis()}_r${retryCount}.jpg"
            val uploadResult = uploadImageWithRetry(compressedUri, uploadPath, retryCount)
            
            when (uploadResult) {
                is CustomResult.Success -> {
                    val imageUrl = uploadResult.data
                    Log.d(TAG, "✅ 이미지 업로드 성공 (재시도: $retryCount): $imageUrl")

                    // 3단계: 이미지 메시지 페이로드 생성
                    val payload = MessagePayload.forImage(
                        content = content,
                        imageUrl = imageUrl,
                        imageFilename = imageUri.lastPathSegment
                    )

                    // 4단계: 메시지 전송
                    sendMessageInternal(senderId, MessageType.TEXT, payload, replyToMessageId)
                }
                is CustomResult.Failure -> {
                    Log.e(TAG, "❌ 이미지 업로드 실패 (재시도: $retryCount)", uploadResult.error)

                    // 재시도 로직: 최대 3회까지 재시도
                    if (retryCount < 3 && isRetryableError(uploadResult.error)) {
                        Log.d(TAG, "🔄 이미지 업로드 재시도 예약: ${retryCount + 1}/3")
                        delay(1000L * (retryCount + 1)) // 지수 백오프
                        return sendImageMessage(
                            senderId,
                            imageUri,
                            content,
                            replyToMessageId,
                            retryCount + 1
                        )
                    } else {
                        // 최대 재시도 횟수 초과 또는 복구 불가능한 오류
                        Log.e(TAG, "❌ 이미지 업로드 최종 실패 (재시도: $retryCount)")

                        // 오프라인 큐에 추가하여 나중에 재시도
                        queueFailedImageMessage(senderId, imageUri, content, replyToMessageId)

                        CustomResult.Failure(
                            ImageUploadException(
                                "이미지 업로드에 실패했습니다. 네트워크 연결을 확인해주세요.",
                                uploadResult.error
                            )
                        )
                    }
                }
                else -> {
                    Log.e(TAG, "❌ 이미지 업로드 알 수 없는 상태: $uploadResult")
                    CustomResult.Failure(Exception("이미지 업로드 실패"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "이미지 메시지 전송 중 예외 (재시도: $retryCount)", e)

            // 예외가 발생한 경우에도 재시도 로직 적용
            if (retryCount < 3 && isRetryableError(e)) {
                Log.d(TAG, "🔄 이미지 전송 예외 재시도: ${retryCount + 1}/3")
                delay(1000L * (retryCount + 1))
                return sendImageMessage(
                    senderId,
                    imageUri,
                    content,
                    replyToMessageId,
                    retryCount + 1
                )
            }
            
            CustomResult.Failure(e)
        }
    }

    /**
     * 이미지 메시지 전송 (다중 이미지) - 병렬 압축 및 업로드, 부분 실패 핸들링
     */
    suspend fun sendImagesMessage(
        senderId: UserId,
        imageUris: List<Uri>,
        content: String = "",
        replyToMessageId: DocumentId? = null,
        retryCount: Int = 0
    ): CustomResult<DocumentId, Exception> {
        Log.d(
            TAG,
            "이미지들 메시지 전송 시도 (재시도: $retryCount): senderId=${senderId.value}, images=${imageUris.size}개, roomId=$roomId"
        )

        return try {
            // 1단계: 모든 이미지를 병렬로 압축
            Log.d(TAG, "🔄 ${imageUris.size}개 이미지 병렬 압축 시작...")
            val compressionOptions = ImageCompressor.CompressionOptions(
                maxWidth = 1920,
                maxHeight = 1920,
                quality = if (retryCount > 0) 70 else 80, // 재시도 시 품질 낮춤
                maxFileSizeBytes = if (retryCount > 0) 1024 * 1024 else 2 * 1024 * 1024 // 재시도 시 더 작게
            )

            val compressedUris =
                ImageCompressor.compressImages(context, imageUris, compressionOptions)
            Log.d(TAG, "✅ ${compressedUris.size}개 이미지 압축 완료")

            // 2단계: 압축된 이미지들을 병렬로 업로드 (재시도 로직 포함)
            Log.d(TAG, "🔄 ${compressedUris.size}개 이미지 병렬 업로드 시작...")
            val uploadResults = coroutineScope {
                compressedUris.mapIndexed { index, compressedUri ->
                    async {
                        uploadImageWithRetryForBatch(
                            compressedUri,
                            index,
                            imageUris[index],
                            retryCount
                        )
                    }
                }.awaitAll()
            }

            // 3단계: 성공한 업로드 결과들만 수집
            val uploadedImages = uploadResults.filterNotNull()
            val failedCount = uploadResults.count { it == null }

            Log.d(TAG, "📊 업로드 완료: ${uploadedImages.size}/${imageUris.size}개 성공, ${failedCount}개 실패")

            when {
                uploadedImages.isNotEmpty() -> {
                    // 부분적으로라도 성공한 경우 성공한 이미지들로 메시지 전송
                    val payload = MessagePayload.forImages(content, uploadedImages)
                    val result =
                        sendMessageInternal(senderId, MessageType.TEXT, payload, replyToMessageId)

                    // 실패한 이미지가 있으면 사용자에게 알림
                    if (failedCount > 0) {
                        Log.w(TAG, "⚠️ ${failedCount}개 이미지 업로드 실패, ${uploadedImages.size}개만 전송됨")
                        // 실패한 이미지들을 오프라인 큐에 추가
                        val failedUris =
                            imageUris.filterIndexed { index, _ -> uploadResults[index] == null }
                        queueFailedImagesMessage(senderId, failedUris, content, replyToMessageId)
                    }

                    result
                }

                retryCount < 2 -> {
                    // 모든 이미지 업로드 실패, 재시도 가능
                    Log.d(TAG, "🔄 모든 이미지 업로드 실패, 재시도 예약: ${retryCount + 1}/2")
                    delay(2000L * (retryCount + 1))
                    return sendImagesMessage(
                        senderId,
                        imageUris,
                        content,
                        replyToMessageId,
                        retryCount + 1
                    )
                }

                else -> {
                    // 최대 재시도 횟수 초과
                    Log.e(TAG, "❌ 모든 이미지 업로드 최종 실패")
                    queueFailedImagesMessage(senderId, imageUris, content, replyToMessageId)
                    CustomResult.Failure(ImageUploadException("모든 이미지 업로드에 실패했습니다. 나중에 다시 시도됩니다."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "이미지들 메시지 전송 중 예외 (재시도: $retryCount)", e)

            if (retryCount < 2 && isRetryableError(e)) {
                Log.d(TAG, "🔄 이미지들 전송 예외 재시도: ${retryCount + 1}/2")
                delay(2000L * (retryCount + 1))
                return sendImagesMessage(
                    senderId,
                    imageUris,
                    content,
                    replyToMessageId,
                    retryCount + 1
                )
            }
            
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

    /**
     * 이미지 업로드 재시도 로직 포함
     */
    private suspend fun uploadImageWithRetry(
        compressedUri: Uri,
        uploadPath: String,
        currentRetryCount: Int
    ): CustomResult<String, Exception> {
        return try {
            fileUseCases.uploadMediaUseCase(compressedUri, uploadPath)
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
    private suspend fun uploadImageWithRetryForBatch(
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
    }

    /**
     * 실패한 단일 이미지 메시지를 오프라인 큐에 추가
     */
    private fun queueFailedImageMessage(
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
    }

    /**
     * 실패한 다중 이미지 메시지를 오프라인 큐에 추가
     */
    private fun queueFailedImagesMessage(
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
    }

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
                        chatImageCache.preloadImages(imageUrls)
                        Log.d(TAG, "✅ 최근 이미지 프리로드 완료: ${imageUrls.size}개")
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

            imageUrls.forEach { imageUrl ->
                chatImageCache.generateThumbnail(imageUrl)
            }

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
            chatImageCache.cleanupOldCache()
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
            append(chatImageCache.getCacheInfo().toString())
        }
    }

    /**
     * 성능 최적화를 위한 이미지 처리 파이프라인
     */
    suspend fun optimizeImagePerformance(imageUrls: List<String>) {
        try {
            Log.d(TAG, "⚡ 이미지 성능 최적화 시작")

            // 1. 이미지 프리로딩
            chatImageCache.preloadImages(imageUrls.take(10)) // 최대 10개

            // 2. 썸네일 생성 (UI 스크롤 성능 향상)
            imageUrls.take(20).forEach { url -> // 최대 20개
                chatImageCache.generateThumbnail(url)
            }

            Log.d(TAG, "✅ 이미지 성능 최적화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 이미지 성능 최적화 중 예외", e)
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