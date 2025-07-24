package com.example.feature_chat.service

import android.net.Uri
import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.core_common.websocket.WebSocketConnectionState
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.model.vo.message.MessageIsDeleted
import com.example.domain.provider.chat.ChatUseCases
import com.example.feature_chat.model.ChatMessageUiModel
import com.example.feature_chat.config.ChatMemoryConfig
import com.example.feature_chat.memory.MessageMemoryManager
import com.example.feature_chat.memory.MemoryUpdateResult
import com.example.feature_chat.queue.OfflineMessageQueue
import com.example.feature_chat.queue.QueuedMessageAction
import com.example.feature_chat.websocket.ChatWebSocketClient
import com.example.feature_chat.websocket.ChatWebSocketEvent
import com.example.domain.model.vo.message.MentionInfo
import com.example.feature_chat.util.MentionParser
import com.example.feature_chat.util.ReplyParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
                        findReplyToMessage = { messageId -> memoryManager.messages.find { it.chatId == messageId } }
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
            // 채널 ID 추출 (roomId에서 "chat_room_" 접두사 제거)
            val channelId = roomId.removePrefix("chat_room_")

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
                        findReplyToMessage = { messageId -> memoryManager.messages.find { it.chatId == messageId } }
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
                                findReplyToMessage = { messageId -> memoryManager.messages.find { it.chatId == messageId } }
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
                            findReplyToMessage = { messageId -> memoryManager.messages.find { it.chatId == messageId } }
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
                        findReplyToMessage = { messageId -> memoryManager.messages.find { it.chatId == messageId } }
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
                        findReplyToMessage = { messageId -> memoryManager.messages.find { it.chatId == messageId } }
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
     * 메시지 전송
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
        
        val messageId = DocumentId.generate()
        Log.d("MessageService", "Sending message: ${text.take(50)}... by user: $senderId")

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
            memoryManager.messages.find { it.chatId == replyId.value }
        }
        
        val tempUiMessage = ChatMessageUiModel(
            localId = generateTempId(),
            chatId = messageId.value,
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

        when (webSocketClient.connectionState.value) {
            is WebSocketConnectionState.Connected -> {
                val result = webSocketClient.sendMessage(
                    roomId = roomId,
                    senderId = UserId(senderId),
                    content = actualMessageContent, // 파싱된 실제 내용 사용
                    messageId = messageId
                )
                
                when {
                    result.isSuccess -> {
                        chatUseCases.sendMessageUseCase(
                            senderId = UserId(senderId),
                            content = MessageContent(actualMessageContent), // 파싱된 실제 내용 사용
                            mentions = mentions
                        )

                        // 로컬 캐시에 저장 (WebSocket 전송 성공 시)
                        if (chatCacheManager != null) {
                            try {
                                val channelId = roomId.removePrefix("chat_room_")
                                chatCacheManager.addRealtimeMessage(channelId, message)
                                Log.d(
                                    "MessageService",
                                    "Successfully saved sent message to local cache: ${message.id.value}"
                                )
                            } catch (e: Exception) {
                                Log.w(
                                    "MessageService",
                                    "Failed to save sent message to local cache: ${message.id.value}",
                                    e
                                )
                            }
                        }
                        
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
                offlineMessageQueue.queueMessage(
                    QueuedMessageAction.Send(message, roomId)
                )

                // 오프라인 상태에서도 로컬 캐시에 저장 (오프라인 메시지)
                if (chatCacheManager != null) {
                    try {
                        val channelId = roomId.removePrefix("chat_room_")
                        chatCacheManager.addRealtimeMessage(channelId, message)
                        Log.d(
                            "MessageService",
                            "Successfully saved offline message to local cache: ${message.id.value}"
                        )
                    } catch (e: Exception) {
                        Log.w(
                            "MessageService",
                            "Failed to save offline message to local cache: ${message.id.value}",
                            e
                        )
                    }
                }
                
                return handleSendMessageFallback(message, tempUiMessage, senderId)
            }
        }
        
        return SendMessageResult(success = false, error = "알 수 없는 오류가 발생했습니다")
    }
    
    /**
     * 메시지 편집
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        if (newContent.isBlank()) {
            Log.w("MessageService", "Attempted to edit message with empty content")
            return Result.failure(Exception("빈 내용으로 메시지를 편집할 수 없습니다"))
        }
        
        Log.d("MessageService", "Editing message: $messageId")
        
        when (webSocketClient.connectionState.value) {
            is WebSocketConnectionState.Connected -> {
                Log.d("MessageService", "Editing message via WebSocket")
                webSocketClient.editMessage(
                    roomId = roomId,
                    messageId = DocumentId(messageId),
                    newContent = newContent
                )
            }
            else -> {
                Log.d("MessageService", "Queueing message edit for offline processing")
                offlineMessageQueue.queueMessage(
                    QueuedMessageAction.Edit(messageId, newContent, roomId)
                )
            }
        }
        
        return when (val result = chatUseCases.editMessageUseCase(DocumentId(messageId), MessageContent(newContent))) {
            is CustomResult.Failure -> {
                Result.failure(Exception("메시지 수정 실패: ${result.error.message}"))
            }
            else -> {
                Result.success(Unit)
            }
        }
    }
    
    /**
     * 메시지 삭제
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        Log.d("MessageService", "Deleting message: $messageId")
        
        when (webSocketClient.connectionState.value) {
            is WebSocketConnectionState.Connected -> {
                Log.d("MessageService", "Deleting message via WebSocket")
                webSocketClient.deleteMessage(roomId, DocumentId(messageId))
            }
            else -> {
                Log.d("MessageService", "Queueing message delete for offline processing")
                offlineMessageQueue.queueMessage(
                    QueuedMessageAction.Delete(messageId, roomId)
                )
            }
        }
        
        return when (val result = chatUseCases.deleteMessageUseCase(DocumentId(messageId))) {
            is CustomResult.Failure -> {
                Result.failure(Exception("메시지 삭제 실패: ${result.error.message}"))
            }
            else -> {
                Result.success(Unit)
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
            localId = generateTempId(),
            chatId = event.messageId,
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
                val channelId = roomId.removePrefix("chat_room_")
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
            if (message.chatId == event.messageId) {
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
                val channelId = roomId.removePrefix("chat_room_")
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
            if (message.chatId == event.messageId) {
                message.copy(isDeleted = true)
            } else {
                null
            }
        }

        // 2. 캐시 매니저가 활성화된 경우 로컬 캐시도 업데이트
        if (chatCacheManager != null) {
            try {
                val channelId = roomId.removePrefix("chat_room_")
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
        when (val result = chatUseCases.sendMessageUseCase(
            senderId = message.senderId, 
            content = message.content, 
            mentions = message.mentions
        )) {
            is CustomResult.Success -> {
                // Ensure profile is loaded for the actual message too
                userProfileService.loadUserProfile(result.data.senderId.value)

                // 로컬 캐시에 저장 (Firestore 전송 성공 시)
                if (chatCacheManager != null) {
                    try {
                        val channelId = roomId.removePrefix("chat_room_")
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
                    findReplyToMessage = { messageId -> memoryManager.messages.find { it.chatId == messageId } }
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
        localId = tempIdGenerator(),
        chatId = this.id.value,
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