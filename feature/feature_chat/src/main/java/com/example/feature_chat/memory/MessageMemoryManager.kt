package com.example.feature_chat.memory

import android.util.Log
import com.example.feature_chat.config.ChatMemoryConfig
import com.example.feature_chat.model.ChatMessageUiModel
import java.time.Instant

/**
 * 메시지 메모리 관리 및 양방향 로딩을 담당하는 클래스
 * 
 * 기능:
 * - 메시지 목록의 메모리 사용량 제한
 * - 양방향 메시지 로딩 (과거/최신)
 * - 메모리 초과 시 자동 정리
 * - 메시지 윈도우 경계 추적
 * - 중복 검사 최적화
 */
class MessageMemoryManager {
    
    // 현재 메모리에 있는 메시지 목록 (timestamp 기준 내림차순 정렬)
    private val _messages = mutableListOf<ChatMessageUiModel>()
    val messages: List<ChatMessageUiModel> get() = _messages.toList()
    
    // 빠른 중복 검사를 위한 Set (chatId 기반)
    private val _messageIds = mutableSetOf<String>()
    
    // 빠른 로컬 ID 검색을 위한 Map
    private val _localIdToMessageMap = mutableMapOf<String, ChatMessageUiModel>()
    
    // 메시지 윈도우 경계 추적
    private var oldestMessageTimestamp: Instant? = null
    private var newestMessageTimestamp: Instant? = null
    
    // 양방향 로딩 상태
    private var canLoadOlderMessages = true
    private var canLoadNewerMessages = true
    
    /**
     * 초기 메시지들을 메모리에 설정
     */
    fun setInitialMessages(messages: List<ChatMessageUiModel>) {
        Log.d("MessageMemoryManager", "Setting initial ${messages.size} messages")
        
        // Clear all data structures
        _messages.clear()
        _messageIds.clear()
        _localIdToMessageMap.clear()
        
        // Add sorted messages and update indices
        val sortedMessages = messages.sortedByDescending { it.actualTimestamp }
        _messages.addAll(sortedMessages)
        
        // Build indices for fast lookups
        sortedMessages.forEach { message ->
            _messageIds.add(message.chatId)
            _localIdToMessageMap[message.localId] = message
        }
        
        updateBoundaries()
        
        Log.d("MessageMemoryManager", "Initial messages set. Boundaries: oldest=${oldestMessageTimestamp}, newest=${newestMessageTimestamp}, indexed: ${_messageIds.size}")
    }
    
    /**
     * 새로운 실시간 메시지 추가 (가장 최신 메시지로 추가)
     */
    fun addNewestMessage(message: ChatMessageUiModel): List<ChatMessageUiModel> {
        Log.d("MessageMemoryManager", "Adding newest message: ${message.chatId}")
        
        // 빠른 중복 검사
        if (_messageIds.contains(message.chatId)) {
            Log.d("MessageMemoryManager", "Message already exists, skipping")
            return messages
        }
        
        // 최신 메시지로 추가 (리스트 맨 앞에)
        _messages.add(0, message)
        _messageIds.add(message.chatId)
        _localIdToMessageMap[message.localId] = message
        
        // 메모리 정리 수행
        val removedMessages = performMemoryCleanupIfNeeded(CleanupDirection.REMOVE_OLDEST)
        
        // 제거된 메시지들의 인덱스도 업데이트
        removedMessages.forEach { removedMessage ->
            _messageIds.remove(removedMessage.chatId)
            _localIdToMessageMap.remove(removedMessage.localId)
        }
        
        updateBoundaries()
        
        Log.d("MessageMemoryManager", "Added newest message. Total: ${_messages.size}, indexed: ${_messageIds.size}")
        return messages
    }
    
    /**
     * 과거 메시지들을 추가 (기존 메시지보다 오래된 메시지들)
     */
    fun addOlderMessages(olderMessages: List<ChatMessageUiModel>): MemoryUpdateResult {
        Log.d("MessageMemoryManager", "Adding ${olderMessages.size} older messages")
        
        if (olderMessages.isEmpty()) {
            return MemoryUpdateResult(
                messages = messages,
                hasMoreOlderMessages = false,
                hasMoreNewerMessages = canLoadNewerMessages,
                removedMessages = emptyList()
            )
        }
        
        // 빠른 중복 제거 및 정렬
        val newMessages = olderMessages
            .filter { newMsg -> !_messageIds.contains(newMsg.chatId) }
            .sortedByDescending { it.actualTimestamp }
        
        // 기존 메시지 뒤에 추가
        _messages.addAll(newMessages)
        _messages.sortByDescending { it.actualTimestamp }
        
        // 인덱스 업데이트
        newMessages.forEach { message ->
            _messageIds.add(message.chatId)
            _localIdToMessageMap[message.localId] = message
        }
        
        // 메모리 정리 수행
        val removedMessages = performMemoryCleanupIfNeeded(CleanupDirection.REMOVE_NEWEST)
        
        updateBoundaries()
        
        // 더 오래된 메시지가 있는지 확인 (로드한 메시지 개수가 페이지 크기와 같으면 더 있을 가능성)
        canLoadOlderMessages = olderMessages.size == ChatMemoryConfig.PAGINATION_SIZE
        
        Log.d("MessageMemoryManager", "Added older messages. Total: ${_messages.size}, canLoadOlder: $canLoadOlderMessages")
        
        return MemoryUpdateResult(
            messages = messages,
            hasMoreOlderMessages = canLoadOlderMessages,
            hasMoreNewerMessages = canLoadNewerMessages,
            removedMessages = removedMessages
        )
    }
    
    /**
     * 최신 메시지들을 추가 (기존 메시지보다 새로운 메시지들)
     */
    fun addNewerMessages(newerMessages: List<ChatMessageUiModel>): MemoryUpdateResult {
        Log.d("MessageMemoryManager", "Adding ${newerMessages.size} newer messages")
        
        if (newerMessages.isEmpty()) {
            return MemoryUpdateResult(
                messages = messages,
                hasMoreOlderMessages = canLoadOlderMessages,
                hasMoreNewerMessages = false,
                removedMessages = emptyList()
            )
        }
        
        // 빠른 중복 제거 및 정렬
        val newMessages = newerMessages
            .filter { newMsg -> !_messageIds.contains(newMsg.chatId) }
            .sortedByDescending { it.actualTimestamp }
        
        // 기존 메시지 앞에 추가
        _messages.addAll(0, newMessages)
        
        // 인덱스 업데이트
        newMessages.forEach { message ->
            _messageIds.add(message.chatId)
            _localIdToMessageMap[message.localId] = message
        }
        
        // 메모리 정리 수행
        val removedMessages = performMemoryCleanupIfNeeded(CleanupDirection.REMOVE_OLDEST)
        
        updateBoundaries()
        
        // 더 새로운 메시지가 있는지 확인
        canLoadNewerMessages = newerMessages.size == ChatMemoryConfig.PAGINATION_SIZE
        
        Log.d("MessageMemoryManager", "Added newer messages. Total: ${_messages.size}, canLoadNewer: $canLoadNewerMessages")
        
        return MemoryUpdateResult(
            messages = messages,
            hasMoreOlderMessages = canLoadOlderMessages,
            hasMoreNewerMessages = canLoadNewerMessages,
            removedMessages = removedMessages
        )
    }
    
    /**
     * 메시지 업데이트 (편집, 삭제 등)
     * 로컬 ID를 통한 빠른 검색으로 성능 최적화
     */
    fun updateMessage(updater: (ChatMessageUiModel) -> ChatMessageUiModel?): List<ChatMessageUiModel> {
        for (i in _messages.indices) {
            val original = _messages[i]
            val updated = updater(original)
            if (updated != null) {
                _messages[i] = updated
                
                // 인덱스 업데이트 (ID가 변경된 경우)
                if (original.chatId != updated.chatId) {
                    _messageIds.remove(original.chatId)
                    _messageIds.add(updated.chatId)
                }
                if (original.localId != updated.localId) {
                    _localIdToMessageMap.remove(original.localId)
                    _localIdToMessageMap[updated.localId] = updated
                } else {
                    _localIdToMessageMap[updated.localId] = updated
                }
                
                Log.d("MessageMemoryManager", "Message updated: ${updated.chatId}")
                break
            }
        }
        return messages
    }
    
    /**
     * 로컬 ID를 통한 빠른 메시지 검색
     */
    fun findMessageByLocalId(localId: String): ChatMessageUiModel? {
        return _localIdToMessageMap[localId]
    }
    
    /**
     * 채팅 ID를 통한 메시지 존재 여부 확인
     */
    fun containsMessage(chatId: String): Boolean {
        return _messageIds.contains(chatId)
    }
    
    /**
     * 특정 메시지 제거
     */
    fun removeMessage(messageId: String): List<ChatMessageUiModel> {
        val iterator = _messages.iterator()
        var removed = false
        
        while (iterator.hasNext()) {
            val message = iterator.next()
            if (message.chatId == messageId) {
                iterator.remove()
                _messageIds.remove(message.chatId)
                _localIdToMessageMap.remove(message.localId)
                removed = true
                Log.d("MessageMemoryManager", "Message removed: $messageId")
                break // 중복이 없으므로 첫 번째 매치에서 중단
            }
        }
        
        if (removed) {
            updateBoundaries()
        }
        
        return messages
    }
    
    /**
     * 메모리 정리가 필요한지 확인하고 수행
     * 최근 전송한 메시지와 전송 중인 메시지는 보호함
     */
    private fun performMemoryCleanupIfNeeded(direction: CleanupDirection): List<ChatMessageUiModel> {
        val removedMessages = mutableListOf<ChatMessageUiModel>()
        
        if (_messages.size > ChatMemoryConfig.MEMORY_CLEANUP_THRESHOLD) {
            Log.d("MessageMemoryManager", "Memory cleanup needed. Current size: ${_messages.size}, threshold: ${ChatMemoryConfig.MEMORY_CLEANUP_THRESHOLD}")
            
            val messagesToRemove = minOf(
                ChatMemoryConfig.MESSAGES_TO_REMOVE_ON_CLEANUP,
                _messages.size - ChatMemoryConfig.MIN_MESSAGES_IN_MEMORY
            )
            
            when (direction) {
                CleanupDirection.REMOVE_OLDEST -> {
                    // 가장 오래된 메시지들 제거 (리스트 끝에서부터)
                    // 단, 최근 전송한 메시지와 전송 중인 메시지는 보호
                    var removed = 0
                    var index = _messages.size - 1
                    
                    while (removed < messagesToRemove && index >= 0 && _messages.size > ChatMemoryConfig.MIN_MESSAGES_IN_MEMORY) {
                        val message = _messages[index]
                        
                        if (shouldProtectMessage(message)) {
                            Log.d("MessageMemoryManager", "Protecting message from cleanup: ${message.localId} (recent or sending)")
                            index--
                            continue
                        }
                        
                        val removedMessage = _messages.removeAt(index)
                        _messageIds.remove(removedMessage.chatId)
                        _localIdToMessageMap.remove(removedMessage.localId)
                        removedMessages.add(removedMessage)
                        removed++
                        index--
                    }
                    
                    Log.d("MessageMemoryManager", "Removed ${removedMessages.size} oldest messages (protected ${messagesToRemove - removed} messages)")
                }
                CleanupDirection.REMOVE_NEWEST -> {
                    // 가장 새로운 메시지들 제거 (리스트 앞에서부터)
                    // 단, 전송 중인 메시지는 보호
                    var removed = 0
                    var index = 0
                    
                    while (removed < messagesToRemove && index < _messages.size && _messages.size > ChatMemoryConfig.MIN_MESSAGES_IN_MEMORY) {
                        val message = _messages[index]
                        
                        if (shouldProtectMessage(message)) {
                            Log.d("MessageMemoryManager", "Protecting message from cleanup: ${message.localId} (recent or sending)")
                            index++
                            continue
                        }
                        
                        val removedMessage = _messages.removeAt(index)
                        _messageIds.remove(removedMessage.chatId)
                        _localIdToMessageMap.remove(removedMessage.localId)
                        removedMessages.add(removedMessage)
                        removed++
                        // index는 증가시키지 않음 (제거된 다음 메시지가 같은 인덱스로 이동)
                    }
                    
                    Log.d("MessageMemoryManager", "Removed ${removedMessages.size} newest messages (protected ${messagesToRemove - removed} messages)")
                }
            }
        }
        
        return removedMessages
    }
    
    /**
     * 메시지가 메모리 정리로부터 보호되어야 하는지 확인
     * - 전송 중인 메시지 (isSending = true)
     * - 최근 30초 내에 전송한 메시지 (clientSentAt 기준)
     * - 낙관적 업데이트 메시지 (isOptimistic = true)
     */
    private fun shouldProtectMessage(message: ChatMessageUiModel): Boolean {
        // 전송 중인 메시지는 항상 보호
        if (message.isSending) {
            return true
        }
        
        // 낙관적 업데이트 메시지는 항상 보호
        if (message.isOptimistic) {
            return true
        }
        
        // 최근 30초 내에 클라이언트에서 전송한 메시지 보호
        val clientSentAt = message.clientSentAt
        if (clientSentAt != null) {
            val timeSinceSent = java.time.Duration.between(clientSentAt, Instant.now())
            if (timeSinceSent.seconds < 30) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * 메시지 윈도우 경계 업데이트
     */
    private fun updateBoundaries() {
        if (_messages.isEmpty()) {
            oldestMessageTimestamp = null
            newestMessageTimestamp = null
        } else {
            newestMessageTimestamp = _messages.first().actualTimestamp
            oldestMessageTimestamp = _messages.last().actualTimestamp
        }
    }
    
    /**
     * 현재 상태 정보 반환
     */
    fun getMemoryInfo(): MemoryInfo {
        return MemoryInfo(
            currentMessageCount = _messages.size,
            maxCapacity = ChatMemoryConfig.MAX_MESSAGES_IN_MEMORY,
            oldestTimestamp = oldestMessageTimestamp,
            newestTimestamp = newestMessageTimestamp,
            canLoadOlder = canLoadOlderMessages,
            canLoadNewer = canLoadNewerMessages
        )
    }
    
    /**
     * 메모리 완전 초기화
     */
    fun clear() {
        _messages.clear()
        oldestMessageTimestamp = null
        newestMessageTimestamp = null
        canLoadOlderMessages = true
        canLoadNewerMessages = true
    }
    
    // 메모리 정리 방향
    private enum class CleanupDirection {
        REMOVE_OLDEST,  // 오래된 메시지 제거 (새 메시지 로딩 시)
        REMOVE_NEWEST   // 새로운 메시지 제거 (과거 메시지 로딩 시)
    }
}

/**
 * 메모리 업데이트 결과
 */
data class MemoryUpdateResult(
    val messages: List<ChatMessageUiModel>,
    val hasMoreOlderMessages: Boolean,
    val hasMoreNewerMessages: Boolean,
    val removedMessages: List<ChatMessageUiModel>
)

/**
 * 메모리 상태 정보
 */
data class MemoryInfo(
    val currentMessageCount: Int,
    val maxCapacity: Int,
    val oldestTimestamp: Instant?,
    val newestTimestamp: Instant?,
    val canLoadOlder: Boolean,
    val canLoadNewer: Boolean
)