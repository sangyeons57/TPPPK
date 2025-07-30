package com.example.feature_chat.service

import android.net.Uri
import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.model.vo.message.MessageIsDeleted
import com.example.domain_usecase.provider.chat.ChatUseCases
import com.example.feature_chat.config.ChatMemoryConfig
import com.example.feature_chat.memory.MessageMemoryManager
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.queue.OfflineMessageQueue
import com.example.feature_chat.queue.QueuedMessageAction
import com.example.feature_chat.util.MentionParser
import com.example.feature_chat.util.ReplyParser
import com.example.feature_chat.websocket.ChatWebSocketClient
import com.example.feature_chat.websocket.ChatWebSocketEvent
import com.example.websocket.WebSocketConnectionState
import kotlinx.coroutines.coroutineScope
import java.time.Instant

/**
 * 메시지 송수신 및 처리를 담당하는 Service
 * 메시지 전송, 편집, 삭제, 과거 메시지 로딩 등의 기능을 제공합니다.
 */
class MessageService(
    private val chatUseCases: ChatUseCases,
    private val webSocketClient: ChatWebSocketClient,
    private val offlineMessageQueue: OfflineMessageQueue,
    private val userProfileService: UserProfileService,
    private val roomId: String,
    private val projectId: String? = null,
    private val channelType: String,
    private val chatCacheManager: com.example.data.cache.ChatCacheManager? = null // 선택적 의존성으로 점진적 롤아웃 지원
) {
    
    // 메모리 관리자
    private val memoryManager = MessageMemoryManager()
    
    private var tempMessageCounter = 0L
    
    data class MessageResult(
        val messages: List<ChatMessageUiModel> = emptyList(),
        val hasMoreMessages: Boolean = true,
        val hasMoreOlderMessages: Boolean = true,
        val hasMoreNewerMessages: Boolean = false,
        val lastMessageTimestamp: Instant? = null,
        val error: String? = null,
        val removedMessages: List<ChatMessageUiModel> = emptyList()
    )
    
    data class SendMessageResult(
        val success: Boolean,
        val tempMessage: ChatMessageUiModel? = null,
        val actualMessage: ChatMessageUiModel? = null,
        val error: String? = null
    )
    
    /**
     * 초기 메시지들을 로딩
     * 캐시 매니저가 활성화된 경우 로컬 캐시를 우선 사용하고 백그라운드에서 동기화
     */
    suspend fun loadInitialMessages(currentUserId: String): MessageResult = coroutineScope {
        val startTime = System.currentTimeMillis()
        Log.d("MessageService", "Loading initial messages - PERFORMANCE START")

        // 캐시 매니저가 활성화된 경우 캐시 우선 전략 사용
        if (chatCacheManager != null) {
            Log.d("MessageService", "Using cache-first strategy with ChatCacheManager")
            return@coroutineScope loadInitialMessagesWithCache(currentUserId, startTime)
        }

        // 기존 방식 (Firestore 직접 접근)
        Log.d("MessageService", "Using legacy Firestore-direct strategy")
        when (val result = chatUseCases.fetchPastMessagesUseCase(limit = ChatMemoryConfig.PAGINATION_SIZE)) {
            is CustomResult.Success -> {
                val fetchTime = System.currentTimeMillis()
                Log.d("MessageService", "SUCCESS - got ${result.data.size} messages in ${fetchTime - startTime}ms, optimizing profile loading")
                
                // 1단계: 현재 메시지에 있는 모든 사용자 ID 수집
                val userIds = result.data.map { it.senderId.value }.toSet()
                val cachedCount = userIds.count { userProfileService.isUserCached(it) }
                Log.d("MessageService", "PERFORMANCE - Found ${userIds.size} unique users in ${result.data.size} messages (${cachedCount} already cached, ${userIds.size - cachedCount} need loading)")
                
                // 2단계: 배치로 사용자 프로필 로딩 (캐시되지 않은 것만)
                val profileLoadStart = System.currentTimeMillis()
                userProfileService.loadUserProfiles(userIds)
                val profileLoadTime = System.currentTimeMillis() - profileLoadStart
                
                // 3단계: 모든 메시지를 UI 모델로 변환 (이제 대부분 캐시된 데이터 사용)
                val uiConversionStart = System.currentTimeMillis()
                val messages = result.data.map { message ->
                    message.toUiModel(
                        currentUserId = currentUserId,
                        tempIdGenerator = ::generateTempId,
                        getUserDisplayName = userProfileService::getUserDisplayName,
                        getUserProfileUrl = userProfileService::getUserProfileUrl,
                        getCachedProfileUrl = userProfileService::getCachedProfileUrl,
                        getUserIdByUsername = userProfileService::getUserIdByUsername,
                        findReplyToMessage = { messageId -> memoryManager.messages.find { it.messageId == messageId } }
                    )
                }
                val uiConversionTime = System.currentTimeMillis() - uiConversionStart
                val totalTime = System.currentTimeMillis() - startTime

                Log.d("MessageService", "PERFORMANCE COMPLETE - Total: ${totalTime}ms | Fetch: ${fetchTime - startTime}ms | Profile Load: ${profileLoadTime}ms | UI Conversion: ${uiConversionTime}ms | Messages: ${messages.size}")
                
                // 메모리 관리자에 초기 메시지 설정
                memoryManager.setInitialMessages(messages)
                val memoryInfo = memoryManager.getMemoryInfo()
                
                MessageResult(
                    messages = messages,
                    hasMoreMessages = messages.size == ChatMemoryConfig.PAGINATION_SIZE,
                    hasMoreOlderMessages = memoryInfo.canLoadOlder,
                    hasMoreNewerMessages = memoryInfo.canLoadNewer,
                    lastMessageTimestamp = messages.lastOrNull()?.actualTimestamp
                )
            }
            is CustomResult.Failure -> {
                Log.e("MessageService", "Failed to load messages", result.error)
                MessageResult(error = "메시지 로드 실패: ${result.error.message}")
            }
            else -> {
                Log.d("MessageService", "Loading initial messages...")
                MessageResult()
            }
        }
    }

    /**
     * 캐시 매니저를 사용한 초기 메시지 로딩
     * 로컬 캐시에서 즉시 반환 후 백그라운드에서 동기화
     */
    private suspend fun loadInitialMessagesWithCache(
        currentUserId: String,
        startTime: Long
    ): MessageResult {
        return try {
            // roomId는 이제 순수한 channelId
            val channelId = roomId

            // 캐시에서 메시지 가져오기 (즉시 반환 + 백그라운드 동기화)
            val cachedMessages =
                chatCacheManager!!.getMessagesWithSync(channelId, ChatMemoryConfig.PAGINATION_SIZE)

            val cacheTime = System.currentTimeMillis()
            Log.d(
                "MessageService",
                "CACHE SUCCESS - got ${cachedMessages.size} cached messages in ${cacheTime - startTime}ms"
            )

            if (cachedMessages.isNotEmpty()) {
                // 사용자 프로필 로딩
                val userIds = cachedMessages.map { it.senderId.value }.toSet()
                userProfileService.loadUserProfiles(userIds)

                // UI 모델로 변환
                val uiMessages = cachedMessages.map { message ->
                    message.toUiModel(
                        currentUserId = currentUserId,
                        tempIdGenerator = ::generateTempId,
                        getUserDisplayName = userProfileService::getUserDisplayName,
                        getUserProfileUrl = userProfileService::getUserProfileUrl,
                        getCachedProfileUrl = userProfileService::getCachedProfileUrl,
                        getUserIdByUsername = userProfileService::getUserIdByUsername,
                        findReplyToMessage = { messageId -> memoryManager.messages.find { it.messageId == messageId } }
                    )
                }

                // 메모리 관리자에 메시지 설정
                memoryManager.setInitialMessages(uiMessages)
                val memoryInfo = memoryManager.getMemoryInfo()

                val totalTime = System.currentTimeMillis() - startTime
                Log.d(
                    "MessageService",
                    "CACHE PERFORMANCE COMPLETE - Total: ${totalTime}ms | Messages: ${uiMessages.size}"
                )

                MessageResult(
                    messages = uiMessages,
                    hasMoreMessages = uiMessages.size == ChatMemoryConfig.PAGINATION_SIZE,
                    hasMoreOlderMessages = memoryInfo.canLoadOlder,
                    hasMoreNewerMessages = memoryInfo.canLoadNewer,
                    lastMessageTimestamp = uiMessages.lastOrNull()?.actualTimestamp
                )
            } else {
                // 캐시가 비어있는 경우 기존 방식으로 폴백
                Log.d("MessageService", "Cache is empty, falling back to Firestore")
                when (val result =
                    chatUseCases.fetchPastMessagesUseCase(limit = ChatMemoryConfig.PAGINATION_SIZE)) {
                    is CustomResult.Success -> {
                        val messages = result.data.map { message ->
                            message.toUiModel(
                                currentUserId = currentUserId,
                                tempIdGenerator = ::generateTempId,
                                getUserDisplayName = userProfileService::getUserDisplayName,
                                getUserProfileUrl = userProfileService::getUserProfileUrl,
                                getCachedProfileUrl = userProfileService::getCachedProfileUrl,
                                getUserIdByUsername = userProfileService::getUserIdByUsername,
                                findReplyToMessage = { messageId -> memoryManager.messages.find { it.messageId == messageId } }
                            )
                        }

                        memoryManager.setInitialMessages(messages)
                        val memoryInfo = memoryManager.getMemoryInfo()

                        MessageResult(
                            messages = messages,
                            hasMoreMessages = messages.size == ChatMemoryConfig.PAGINATION_SIZE,
                            hasMoreOlderMessages = memoryInfo.canLoadOlder,
                            hasMoreNewerMessages = memoryInfo.canLoadNewer,
                            lastMessageTimestamp = messages.lastOrNull()?.actualTimestamp
                        )
                    }

                    is CustomResult.Failure -> {
                        MessageResult(error = "메시지 로드 실패: ${result.error.message}")
                    }

                    else -> {
                        MessageResult()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MessageService", "Cache-based loading failed, falling back to legacy method", e)
            // 캐시 실패 시 기존 방식으로 완전 폴백
            when (val result =
                chatUseCases.fetchPastMessagesUseCase(limit = ChatMemoryConfig.PAGINATION_SIZE)) {
                is CustomResult.Success -> {
                    val messages = result.data.map { message ->
                        message.toUiModel(
                            currentUserId = currentUserId,
                            tempIdGenerator = ::generateTempId,
                            getUserDisplayName = userProfileService::getUserDisplayName,
                            getUserProfileUrl = userProfileService::getUserProfileUrl,
                            getCachedProfileUrl = userProfileService::getCachedProfileUrl,
                            getUserIdByUsername = userProfileService::getUserIdByUsername,
                            findReplyToMessage = { messageId -> memoryManager.messages.find { it.messageId == messageId } }
                        )
                    }

                    memoryManager.setInitialMessages(messages)
                    val memoryInfo = memoryManager.getMemoryInfo()

                    MessageResult(
                        messages = messages,
                        hasMoreMessages = messages.size == ChatMemoryConfig.PAGINATION_SIZE,
                        hasMoreOlderMessages = memoryInfo.canLoadOlder,
                        hasMoreNewerMessages = memoryInfo.canLoadNewer,
                        lastMessageTimestamp = messages.lastOrNull()?.actualTimestamp
                    )
                }

                is CustomResult.Failure -> {
                    MessageResult(error = "메시지 로드 실패: ${result.error.message}")
                }

                else -> {
                    MessageResult()
                }
            }
        }
    }
    
    /**
     * 과거 메시지들을 로딩 (더 오래된 메시지)
     */
    suspend fun loadOlderMessages(currentUserId: String): MessageResult = coroutineScope {
        val memoryInfo = memoryManager.getMemoryInfo()
        Log.d("MessageService", "Loading older messages before: ${memoryInfo.oldestTimestamp}")
        
        if (!memoryInfo.canLoadOlder) {
            Log.d("MessageService", "Cannot load older messages")
            return@coroutineScope MessageResult(
                messages = memoryManager.messages,
                hasMoreOlderMessages = false,
                hasMoreNewerMessages = memoryInfo.canLoadNewer
            )
        }
        
        val result = chatUseCases.fetchPastMessagesUseCase(
            limit = ChatMemoryConfig.PAGINATION_SIZE,
            beforeTimestamp = memoryInfo.oldestTimestamp
        )
        
        when (result) {
            is CustomResult.Success -> {
                Log.d("MessageService", "SUCCESS - got ${result.data.size} older messages, optimizing profile loading")
                
                // 배치로 사용자 프로필 로딩 (캐시되지 않은 것만)
                val userIds = result.data.map { it.senderId.value }.toSet()
                Log.d("MessageService", "Found ${userIds.size} unique users in older messages")
                userProfileService.loadUserProfiles(userIds)
                
                val uiMessages = result.data.map { message ->
                    message.toUiModel(
                        currentUserId = currentUserId,
                        tempIdGenerator = ::generateTempId,
                        getUserDisplayName = userProfileService::getUserDisplayName,
                        getUserProfileUrl = userProfileService::getUserProfileUrl,
                        getCachedProfileUrl = userProfileService::getCachedProfileUrl,
                        getUserIdByUsername = userProfileService::getUserIdByUsername,
                        findReplyToMessage = { messageId -> memoryManager.messages.find { it.messageId == messageId } }
                    )
                }
                
                // 메모리 관리자에 과거 메시지 추가
                val updateResult = memoryManager.addOlderMessages(uiMessages)
                
                Log.d("MessageService", "Added ${uiMessages.size} older messages, total in memory: ${updateResult.messages.size}")
                if (updateResult.removedMessages.isNotEmpty()) {
                    Log.d("MessageService", "Removed ${updateResult.removedMessages.size} newest messages due to memory limit")
                }
                
                MessageResult(
                    messages = updateResult.messages,
                    hasMoreMessages = updateResult.hasMoreOlderMessages, // 레거시 지원
                    hasMoreOlderMessages = updateResult.hasMoreOlderMessages,
                    hasMoreNewerMessages = updateResult.hasMoreNewerMessages,
                    removedMessages = updateResult.removedMessages,
                    lastMessageTimestamp = updateResult.messages.lastOrNull()?.actualTimestamp
                )
            }
            is CustomResult.Failure -> {
                Log.e("MessageService", "Failed to load older messages", result.error)
                MessageResult(error = "과거 메시지 로드 실패: ${result.error.message}")
            }
            else -> {
                MessageResult(
                    messages = memoryManager.messages,
                    hasMoreOlderMessages = memoryInfo.canLoadOlder,
                    hasMoreNewerMessages = memoryInfo.canLoadNewer
                )
            }
        }
    }
    
    /**
     * 최신 메시지들을 로딩 (더 새로운 메시지)
     */
    suspend fun loadNewerMessages(currentUserId: String): MessageResult = coroutineScope {
        val memoryInfo = memoryManager.getMemoryInfo()
        Log.d("MessageService", "Loading newer messages after: ${memoryInfo.newestTimestamp}")
        
        if (!memoryInfo.canLoadNewer) {
            Log.d("MessageService", "Cannot load newer messages")
            return@coroutineScope MessageResult(
                messages = memoryManager.messages,
                hasMoreOlderMessages = memoryInfo.canLoadOlder,
                hasMoreNewerMessages = false
            )
        }
        
        val newestTimestamp = memoryInfo.newestTimestamp
        if (newestTimestamp == null) {
            Log.w("MessageService", "Cannot load newer messages: no newest timestamp available")
            return@coroutineScope MessageResult(
                messages = memoryManager.messages,
                hasMoreOlderMessages = memoryInfo.canLoadOlder,
                hasMoreNewerMessages = false
            )
        }
        
        val result = chatUseCases.fetchNewerMessagesUseCase(
            afterTimestamp = newestTimestamp,
            limit = ChatMemoryConfig.PAGINATION_SIZE
        )
        
        when (result) {
            is CustomResult.Success -> {
                Log.d("MessageService", "SUCCESS - got ${result.data.size} newer messages, optimizing profile loading")
                
                // 배치로 사용자 프로필 로딩 (캐시되지 않은 것만)
                val userIds = result.data.map { it.senderId.value }.toSet()
                Log.d("MessageService", "Found ${userIds.size} unique users in newer messages")
                userProfileService.loadUserProfiles(userIds)
                
                val uiMessages = result.data.map { message ->
                    message.toUiModel(
                        currentUserId = currentUserId,
                        tempIdGenerator = ::generateTempId,
                        getUserDisplayName = userProfileService::getUserDisplayName,
                        getUserProfileUrl = userProfileService::getUserProfileUrl,
                        getCachedProfileUrl = userProfileService::getCachedProfileUrl,
                        getUserIdByUsername = userProfileService::getUserIdByUsername,
                        findReplyToMessage = { messageId -> memoryManager.messages.find { it.messageId == messageId } }
                    )
                }
                
                // 메모리 관리자에 최신 메시지 추가
                val updateResult = memoryManager.addNewerMessages(uiMessages)
                
                Log.d("MessageService", "Added ${uiMessages.size} newer messages, total in memory: ${updateResult.messages.size}")
                if (updateResult.removedMessages.isNotEmpty()) {
                    Log.d("MessageService", "Removed ${updateResult.removedMessages.size} oldest messages due to memory limit")
                }
                
                MessageResult(
                    messages = updateResult.messages,
                    hasMoreMessages = updateResult.hasMoreOlderMessages, // 레거시 지원
                    hasMoreOlderMessages = updateResult.hasMoreOlderMessages,
                    hasMoreNewerMessages = updateResult.hasMoreNewerMessages,
                    removedMessages = updateResult.removedMessages,
                    lastMessageTimestamp = updateResult.messages.lastOrNull()?.actualTimestamp
                )
            }
            is CustomResult.Failure -> {
                Log.e("MessageService", "Failed to load newer messages", result.error)
                MessageResult(error = "최신 메시지 로드 실패: ${result.error.message}")
            }
            else -> {
                MessageResult(
                    messages = memoryManager.messages,
                    hasMoreOlderMessages = memoryInfo.canLoadOlder,
                    hasMoreNewerMessages = memoryInfo.canLoadNewer
                )
            }
        }
    }
    
    /**
     * 레거시 지원: 기존 loadMoreMessages 메소드 (내부적으로 loadOlderMessages 호출)
     */
    @Deprecated("Use loadOlderMessages() instead")
    suspend fun loadMoreMessages(
        currentUserId: String,
        beforeTimestamp: Instant?
    ): MessageResult = loadOlderMessages(currentUserId)
    
    /**
     * 메시지 전송 - 낙관적 UI 패턴 적용
     * 클라이언트가 생성한 고유 ID를 사용하여 중복 저장 방지
     */
    suspend fun sendMessage(
        text: String,
        attachmentUris: List<Uri>,
        senderId: String,
        replyToMessageId: String? = null // 답장 대상 메시지 ID 추가
    ): SendMessageResult {
        if (text.isBlank() && attachmentUris.isEmpty()) {
            Log.w("MessageService", "Attempted to send empty message")
            return SendMessageResult(success = false, error = "빈 메시지는 전송할 수 없습니다")
        }

        // 클라이언트 주도 ID 생성 - UUID 기반으로 전역 고유성 보장
        val messageId = DocumentId.generate()
        Log.d(
            "MessageService",
            "Sending message with client-generated ID: ${messageId.value} by user: $senderId"
        )

        // 답장 정보 파싱 (text에서 >>messageId 패턴 확인 또는 파라미터 사용)
        val parsedReplyToId =
            ReplyParser.parseReplyToMessageId(text) ?: replyToMessageId?.let { DocumentId(it) }
        val actualMessageContent = if (ReplyParser.isReplyMessage(text)) {
            ReplyParser.extractMessageContent(text)
        } else {
            text
        }

        // 멘션 파싱
        val mentions = MentionParser.parseAllMentions(actualMessageContent) { username ->
            userProfileService.getUserIdByUsername(username)
        }

        Log.d(
            "MessageService",
            "Parsed ${mentions.size} mentions and reply to: ${parsedReplyToId?.value}"
        )
        
        // 사용자 프로필 로딩 (비동기)
        userProfileService.loadUserProfile(senderId)
        val profileUrl = userProfileService.getUserProfileUrl(senderId)
        val userName = userProfileService.getUserDisplayName(senderId)
        
        val sendTime = Instant.now()

        // 답장 대상 메시지 정보 가져오기 (UI 표시용)
        val replyToMessage = parsedReplyToId?.let { replyId ->
            memoryManager.messages.find { it.messageId == replyId.value }
        }
        
        val tempUiMessage = ChatMessageUiModel(
            messageId = messageId.value,
            userId = senderId,
            userName = userName,
            userProfileUrl = profileUrl,
            message = actualMessageContent, // 파싱된 실제 메시지 내용 사용
            formattedTimestamp = "전송 중...",
            isMyMessage = true,
            isModified = false,
            attachmentImageUrls = attachmentUris.map { it.toString() },
            isDeleted = false,
            actualTimestamp = sendTime,
            isOptimistic = true,
            isSending = true,
            clientSentAt = sendTime,
            // 답장 정보
            replyToMessageId = parsedReplyToId?.value,
            replyToContent = replyToMessage?.message,
            replyToUserName = replyToMessage?.userName,
            // 멘션 정보
            mentions = mentions,
            isMentionedMessage = false // 내 메시지이므로 false
        )
        
        val message = Message.create(
            id = messageId,
            senderId = UserId(senderId),
            content = MessageContent(actualMessageContent), // 파싱된 실제 내용 사용
            replyToMessageId = parsedReplyToId,
            mentions = mentions
        )

        // 서버 중심 저장: 낙관적 로컬 저장 제거
        // 서버에서 Firestore 저장 후 WebSocket으로 브로드캐스트하면 handleNewMessage에서 처리

        when (webSocketClient.connectionState.value) {
            is WebSocketConnectionState.Connected -> {
                val result = webSocketClient.sendMessage(
                    roomId = roomId,
                    senderId = UserId(senderId),
                    content = actualMessageContent, // 파싱된 실제 내용 사용
                    messageId = messageId,
                    projectId = projectId,
                    channelType = channelType
                )
                
                when {
                    result.isSuccess -> {
                        // 서버 중심 저장: WebSocket으로만 전송하고 서버에서 Firestore 저장
                        Log.d(
                            "MessageService",
                            "Message sent via WebSocket successfully: ${message.id.value}"
                        )
                        
                        return SendMessageResult(success = true, tempMessage = tempUiMessage)
                    }
                    result.isFailure -> {
                        return handleSendMessageFallback(message, tempUiMessage, senderId)
                    }
                }
            }
            is WebSocketConnectionState.Disconnected,
            is WebSocketConnectionState.Connecting,
            is WebSocketConnectionState.Error -> {
                Log.d(
                    "MessageService",
                    "WebSocket unavailable, queuing message and using Firestore: ${message.id.value}"
                )

                // 오프라인 큐에 추가 (WebSocket 복구 시 재시도용)
                offlineMessageQueue.queueMessage(
                    QueuedMessageAction.Send(message, roomId)
                )

                // Firestore 직접 저장 (fallback)
                return handleSendMessageFallback(message, tempUiMessage, senderId)
            }
        }
        
        return SendMessageResult(success = false, error = "알 수 없는 오류가 발생했습니다")
    }
    
    /**
     * 메시지 편집 - 서버 중심 저장
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        if (newContent.isBlank()) {
            Log.w("MessageService", "Attempted to edit message with empty content")
            return Result.failure(Exception("빈 내용으로 메시지를 편집할 수 없습니다"))
        }

        Log.d("MessageService", "Editing message via WebSocket only: $messageId")

        return when (webSocketClient.connectionState.value) {
            is WebSocketConnectionState.Connected -> {
                Log.d("MessageService", "Sending edit message to server via WebSocket")
                val result = webSocketClient.editMessage(
                    roomId = roomId,
                    messageId = DocumentId(messageId),
                    newContent = newContent,
                    projectId = projectId,
                    channelType = channelType
                )

                if (result.isSuccess) {
                    Log.d("MessageService", "Edit message sent successfully via WebSocket")
                    Result.success(Unit)
                } else {
                    Log.w("MessageService", "Failed to send edit message via WebSocket")
                    Result.failure(Exception("메시지 수정 전송 실패"))
                }
            }
            else -> {
                Log.d("MessageService", "WebSocket unavailable, queuing message edit")
                offlineMessageQueue.queueMessage(
                    QueuedMessageAction.Edit(messageId, newContent, roomId)
                )
                Result.success(Unit) // 큐에 저장되었으므로 성공으로 처리
            }
        }
    }
    
    /**
     * 메시지 삭제 - 서버 중심 저장
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        Log.d("MessageService", "Deleting message via WebSocket only: $messageId")

        return when (webSocketClient.connectionState.value) {
            is WebSocketConnectionState.Connected -> {
                Log.d("MessageService", "Sending delete message to server via WebSocket")
                val result = webSocketClient.deleteMessage(
                    roomId = roomId,
                    messageId = DocumentId(messageId),
                    projectId = projectId,
                    channelType = channelType
                )

                if (result.isSuccess) {
                    Log.d("MessageService", "Delete message sent successfully via WebSocket")
                    Result.success(Unit)
                } else {
                    Log.w("MessageService", "Failed to send delete message via WebSocket")
                    Result.failure(Exception("메시지 삭제 전송 실패"))
                }
            }
            else -> {
                Log.d("MessageService", "WebSocket unavailable, queuing message delete")
                offlineMessageQueue.queueMessage(
                    QueuedMessageAction.Delete(messageId, roomId)
                )
                Result.success(Unit) // 큐에 저장되었으므로 성공으로 처리
            }
        }
    }
    
    /**
     * 새 메시지 이벤트 처리 - 참여 방식 (Participation-based)
     * 서버에서 브로드캐스트된 모든 메시지를 동일하게 처리
     * 캐시 매니저가 활성화된 경우 로컬 캐시에도 저장
     */
    suspend fun handleNewMessage(
        event: ChatWebSocketEvent.MessageReceived,
        currentUserId: String
    ): MessageResult {
        Log.d("MessageService", "handleNewMessage: received message ${event.messageId} from ${event.senderId}")
        
        // 중복 메시지 방지 (ID 기반)
        if (memoryManager.containsMessage(event.messageId)) {
            Log.d("MessageService", "Message already exists, skipping: ${event.messageId}")
            val memoryInfo = memoryManager.getMemoryInfo()
            return MessageResult(
                messages = memoryManager.messages,
                hasMoreMessages = memoryInfo.canLoadOlder,
                hasMoreOlderMessages = memoryInfo.canLoadOlder,
                hasMoreNewerMessages = memoryInfo.canLoadNewer
            )
        }
        
        // 참여 방식: 모든 메시지를 동일하게 처리 (내 메시지든 남의 메시지든)
        Log.d("MessageService", "Adding new message from ${if (event.senderId == currentUserId) "myself" else "other user"}: ${event.senderId}")
        
        userProfileService.loadUserProfile(event.senderId)
        
        val profileUrl = userProfileService.getUserProfileUrl(event.senderId)
        val userName = userProfileService.getUserDisplayName(event.senderId)

        // 멘션 파싱
        val mentions = MentionParser.parseAllMentions(event.content) { username ->
            userProfileService.getUserIdByUsername(username)
        }

        // 현재 사용자가 멘션되었는지 확인
        val isMentionedMessage = MentionParser.isUserMentioned(mentions, currentUserId)

        // TODO: 답장 정보 파싱 (ChatWebSocketEvent.MessageReceived에 답장 정보 추가 필요)
        // val replyToMessageId = event.replyToMessageId
        // val replyToMessage = replyToMessageId?.let { replyId ->
        //     memoryManager.messages.find { it.chatId == replyId }
        // }
        
        val newMessage = ChatMessageUiModel(
            messageId = event.messageId,
            userId = event.senderId,
            userName = userName,
            userProfileUrl = profileUrl,
            message = event.content,
            formattedTimestamp = DateTimeUtil.formatChatTime(Instant.parse(event.timestamp)),
            isMyMessage = event.senderId == currentUserId,
            isModified = false,
            attachmentImageUrls = emptyList(),
            isDeleted = false,
            actualTimestamp = Instant.parse(event.timestamp),
            isOptimistic = false, // 서버에서 온 확정된 메시지
            isSending = false,
            sendFailed = false,
            // 멘션 정보
            mentions = mentions,
            isMentionedMessage = isMentionedMessage
            // 답장 정보 (향후 ChatWebSocketEvent 확장 시 추가)
            // replyToMessageId = replyToMessageId,
            // replyToContent = replyToMessage?.message,
            // replyToUserName = replyToMessage?.userName
        )

        // 캐시 매니저가 활성화된 경우 로컬 캐시에도 저장
        if (chatCacheManager != null) {
            try {
                val channelId = roomId  // roomId는 이제 순수한 channelId
                val domainMessage = event.toDomainMessage { username ->
                    userProfileService.getUserIdByUsername(username)
                }
                chatCacheManager.addRealtimeMessage(channelId, domainMessage)
                Log.d(
                    "MessageService",
                    "Successfully saved realtime message to cache: ${event.messageId}"
                )
            } catch (e: Exception) {
                Log.w(
                    "MessageService",
                    "Failed to save realtime message to cache: ${event.messageId}",
                    e
                )
                // 캐시 실패는 치명적이지 않으므로 계속 진행
            }
        }
        
        // 메모리 관리자에 새 메시지 추가
        val updatedMessages = memoryManager.addNewestMessage(newMessage)
        val memoryInfo = memoryManager.getMemoryInfo()
        
        Log.d("MessageService", "Added confirmed message. Total messages in memory: ${updatedMessages.size}")
        
        return MessageResult(
            messages = updatedMessages,
            hasMoreMessages = memoryInfo.canLoadOlder,
            hasMoreOlderMessages = memoryInfo.canLoadOlder,
            hasMoreNewerMessages = memoryInfo.canLoadNewer
        )
    }
    
    /**
     * 메시지 편집 이벤트 처리
     * 메모리와 캐시를 모두 업데이트
     */
    suspend fun handleMessageEdit(event: ChatWebSocketEvent.MessageEdited): MessageResult {
        // 1. 메모리에서 메시지 업데이트
        val updatedMessages = memoryManager.updateMessage { message ->
            if (message.messageId == event.messageId) {
                message.copy(
                    message = event.newContent,
                    isModified = true,
                    formattedTimestamp = DateTimeUtil.formatChatTime(Instant.parse(event.timestamp))
                )
            } else {
                null
            }
        }

        // 2. 캐시 매니저가 활성화된 경우 로컬 캐시도 업데이트
        if (chatCacheManager != null) {
            try {
                val channelId = roomId  // roomId는 이제 순수한 channelId
                val updatedDomainMessage = Message.fromDataSource(
                    id = DocumentId(event.messageId),
                    senderId = UserId(event.senderId),
                    content = MessageContent(event.newContent),
                    replyToMessageId = null,
                    createdAt = Instant.parse(event.timestamp),
                    updatedAt = Instant.parse(event.timestamp),
                    isDeleted = MessageIsDeleted.FALSE,
                    mentions = emptyList()
                )
                chatCacheManager.updateRealtimeMessage(channelId, updatedDomainMessage)
                Log.d(
                    "MessageService",
                    "Successfully updated edited message in cache: ${event.messageId}"
                )
            } catch (e: Exception) {
                Log.w(
                    "MessageService",
                    "Failed to update edited message in cache: ${event.messageId}",
                    e
                )
            }
        }
        
        val memoryInfo = memoryManager.getMemoryInfo()
        return MessageResult(
            messages = updatedMessages,
            hasMoreMessages = memoryInfo.canLoadOlder,
            hasMoreOlderMessages = memoryInfo.canLoadOlder,
            hasMoreNewerMessages = memoryInfo.canLoadNewer
        )
    }
    
    /**
     * 메시지 삭제 이벤트 처리
     * 메모리와 캐시를 모두 업데이트
     */
    suspend fun handleMessageDelete(event: ChatWebSocketEvent.MessageDeleted): MessageResult {
        // 1. 메모리에서 메시지 삭제 표시
        val updatedMessages = memoryManager.updateMessage { message ->
            if (message.messageId == event.messageId) {
                message.copy(isDeleted = true)
            } else {
                null
            }
        }

        // 2. 캐시 매니저가 활성화된 경우 로컬 캐시도 업데이트
        if (chatCacheManager != null) {
            try {
                val channelId = roomId  // roomId는 이제 순수한 channelId
                val deletedDomainMessage = Message.fromDataSource(
                    id = DocumentId(event.messageId),
                    senderId = UserId(event.senderId),
                    content = MessageContent(""), // 삭제된 메시지는 내용 비움
                    replyToMessageId = null,
                    createdAt = Instant.parse(event.timestamp),
                    updatedAt = Instant.parse(event.timestamp),
                    isDeleted = MessageIsDeleted.TRUE, // 삭제 표시
                    mentions = emptyList()
                )
                chatCacheManager.updateRealtimeMessage(channelId, deletedDomainMessage)
                Log.d(
                    "MessageService",
                    "Successfully updated deleted message in cache: ${event.messageId}"
                )
            } catch (e: Exception) {
                Log.w(
                    "MessageService",
                    "Failed to update deleted message in cache: ${event.messageId}",
                    e
                )
            }
        }
        
        val memoryInfo = memoryManager.getMemoryInfo()
        return MessageResult(
            messages = updatedMessages,
            hasMoreMessages = memoryInfo.canLoadOlder,
            hasMoreOlderMessages = memoryInfo.canLoadOlder,
            hasMoreNewerMessages = memoryInfo.canLoadNewer
        )
    }
    
    private suspend fun handleSendMessageFallback(
        message: Message,
        tempUiMessage: ChatMessageUiModel,
        senderId: String
    ): SendMessageResult {
        Log.d(
            "MessageService",
            "WebSocket failed, using Firestore fallback for: ${message.id.value}"
        )

        // WebSocket 실패 시 Firestore로 직접 전송 (fallback)
        when (val result = chatUseCases.sendMessageUseCase(message)) {
            is CustomResult.Success -> {
                Log.d("MessageService", "Message sent via Firestore fallback: ${message.id.value}")
                
                // Ensure profile is loaded for the actual message too
                userProfileService.loadUserProfile(result.data.senderId.value)

                // 로컬 캐시에 저장 (Firestore 전송 성공 시)
                if (chatCacheManager != null) {
                    try {
                        val channelId = roomId  // roomId는 이제 순수한 channelId
                        chatCacheManager.addRealtimeMessage(channelId, result.data)
                        Log.d(
                            "MessageService",
                            "Successfully saved fallback message to local cache: ${result.data.id.value}"
                        )
                    } catch (e: Exception) {
                        Log.w(
                            "MessageService",
                            "Failed to save fallback message to local cache: ${result.data.id.value}",
                            e
                        )
                    }
                }
                
                val actualMessage = result.data.toUiModel(
                    currentUserId = senderId,
                    tempIdGenerator = ::generateTempId,
                    getUserDisplayName = userProfileService::getUserDisplayName,
                    getUserProfileUrl = userProfileService::getUserProfileUrl,
                    getCachedProfileUrl = userProfileService::getCachedProfileUrl,
                    getUserIdByUsername = userProfileService::getUserIdByUsername,
                    findReplyToMessage = { messageId -> memoryManager.messages.find { it.messageId == messageId } }
                )
                
                return SendMessageResult(
                    success = true,
                    tempMessage = tempUiMessage,
                    actualMessage = actualMessage
                )
            }
            is CustomResult.Failure -> {
                return SendMessageResult(
                    success = false,
                    tempMessage = tempUiMessage,
                    error = "메시지 전송 실패: ${result.error.message}"
                )
            }
            else -> {
                return SendMessageResult(success = false, error = "메시지 전송 중...")
            }
        }
    }
    
    private fun generateTempId(): String = "temp_${++tempMessageCounter}"
    
    /**
     * 실패한 메시지를 재전송합니다.
     */
    suspend fun retryMessage(
        messageId: String,
        content: String,
        senderId: String,
        roomId: String,
        attachmentUris: List<Uri> = emptyList(),
        replyToMessageId: String? = null
    ): SendMessageResult {
        Log.d("MessageService", "Retrying message: $messageId")

        // 현재는 첨부파일과 답장 미지원으로 단순한 텍스트 메시지만 재전송
        if (attachmentUris.isNotEmpty()) {
            return SendMessageResult(
                success = false,
                error = "첨부파일이 있는 메시지의 재전송은 현재 지원되지 않습니다"
            )
        }

        return try {
            // WebSocket 연결 상태 확인
            when (webSocketClient.connectionState.value) {
                is WebSocketConnectionState.Connected -> {
                    // WebSocket으로 재전송 시도
                    val result = webSocketClient.sendMessage(
                        roomId = roomId,
                        senderId = UserId(senderId),
                        content = content,
                        messageId = DocumentId(messageId),
                        projectId = projectId,
                        channelType = channelType
                    )
                    if (result.isSuccess) {
                        Log.d("MessageService", "Message retry sent via WebSocket: $messageId")
                        SendMessageResult(success = true)
                    } else {
                        val error = result.exceptionOrNull()
                        Log.w(
                            "MessageService",
                            "WebSocket retry failed for: $messageId, error: $error"
                        )

                        // WebSocket 실패 시 Firestore로 폴백
                        retryViaFirestore(messageId, content, senderId)
                    }
                }

                else -> {
                    Log.d(
                        "MessageService",
                        "WebSocket disconnected, retrying via Firestore: $messageId"
                    )

                    // WebSocket 연결 안됨 - Firestore로 직접 재전송
                    retryViaFirestore(messageId, content, senderId)
                }
            }
        } catch (e: Exception) {
            Log.e("MessageService", "Error retrying message: $messageId", e)
            SendMessageResult(
                success = false,
                error = "재전송 중 오류 발생: ${e.message}"
            )
        }
    }

    /**
     * Firestore를 통한 메시지 재전송
     */
    private suspend fun retryViaFirestore(
        messageId: String,
        content: String,
        senderId: String
    ): SendMessageResult {
        return try {
            // 멘션 파싱
            val mentions = MentionParser.parseAllMentions(content) { null }

            // Firestore에 직접 저장
            val result = chatUseCases.sendMessageUseCase(
                senderId = UserId(senderId),
                content = MessageContent(content),
                mentions = mentions
            )

            when (result) {
                is CustomResult.Success -> {
                    Log.d("MessageService", "Message retry succeeded via Firestore: $messageId")

                    // 성공시 로컬 캐시에도 저장
                    if (chatCacheManager != null) {
                        try {
                            val channelId = roomId  // roomId는 이제 순수한 channelId
                            chatCacheManager.addRealtimeMessage(channelId, result.data)
                        } catch (e: Exception) {
                            Log.w("MessageService", "Failed to cache retry message: $messageId", e)
                        }
                    }

                    SendMessageResult(
                        success = true
                    )
                }

                is CustomResult.Failure -> {
                    Log.w(
                        "MessageService",
                        "Firestore retry failed for: $messageId, error: ${result.error}"
                    )
                    SendMessageResult(
                        success = false,
                        error = "Firestore 재전송 실패: ${result.error}"
                    )
                }

                else -> {
                    SendMessageResult(success = false, error = "재전송 처리 중...")
                }
            }
        } catch (e: Exception) {
            Log.e("MessageService", "Firestore retry error for: $messageId", e)
            SendMessageResult(
                success = false,
                error = "Firestore 재전송 오류: ${e.message}"
            )
        }
    }

    /**
     * 현재 메모리 상태 정보 조회
     */
    fun getMemoryInfo() = memoryManager.getMemoryInfo()
    
    /**
     * 메모리에서 현재 보관중인 메시지 목록 조회
     */
    fun getCurrentMessages(): List<ChatMessageUiModel> = memoryManager.messages
    
    /**
     * 메모리 초기화
     */
    fun clearMemory() {
        memoryManager.clear()
    }
}

/**
 * ChatWebSocketEvent.MessageReceived를 도메인 Message로 변환하는 확장 함수
 */
private suspend fun ChatWebSocketEvent.MessageReceived.toDomainMessage(
    getUserIdByUsername: suspend (String) -> String?
): Message {
    // 멘션 파싱
    val mentions = MentionParser.parseAllMentions(content, getUserIdByUsername)

    return Message.fromDataSource(
        id = DocumentId(messageId),
        senderId = UserId(senderId),
        content = MessageContent(content),
        replyToMessageId = null, // TODO: ChatWebSocketEvent에 답장 정보 추가 필요
        createdAt = Instant.parse(timestamp),
        updatedAt = Instant.parse(timestamp),
        isDeleted = MessageIsDeleted.FALSE,
        mentions = mentions
    )
}

private suspend fun Message.toUiModel(
    currentUserId: String,
    tempIdGenerator: () -> String,
    getUserDisplayName: (String) -> String,
    getUserProfileUrl: suspend (String) -> String?,
    getCachedProfileUrl: (String) -> String?,
    getUserIdByUsername: suspend (String) -> String? = { null }, // 멘션 파싱용
    findReplyToMessage: (String) -> ChatMessageUiModel? = { null } // 답장 대상 메시지 찾기용
): ChatMessageUiModel {
    val isModified = this.updatedAt.isAfter(this.createdAt.plusSeconds(1))

    // 멘션 파싱
    val mentions = MentionParser.parseAllMentions(this.content.value, getUserIdByUsername)
    val isMentionedMessage = MentionParser.isUserMentioned(mentions, currentUserId)

    // 답장 정보 처리
    val replyToMessage = this.replyToMessageId?.let { replyId ->
        findReplyToMessage(replyId.value)
    }
    
    return ChatMessageUiModel(
        messageId = this.id.value,
        userId = this.senderId.value,
        userName = getUserDisplayName(this.senderId.value),
        userProfileUrl = getCachedProfileUrl(this.senderId.value) ?: getUserProfileUrl(this.senderId.value),
        message = this.content.value,
        formattedTimestamp = DateTimeUtil.formatChatTime(this.createdAt),
        isModified = isModified,
        attachmentImageUrls = emptyList(),
        isMyMessage = this.senderId.value == currentUserId,
        isDeleted = this.isDeleted.value,
        actualTimestamp = this.createdAt,
        isOptimistic = false,
        isSending = false,
        clientSentAt = null,
        // 답장 정보
        replyToMessageId = this.replyToMessageId?.value,
        replyToContent = replyToMessage?.message,
        replyToUserName = replyToMessage?.userName,
        // 멘션 정보
        mentions = mentions,
        isMentionedMessage = isMentionedMessage
    )
}
