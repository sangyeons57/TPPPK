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
 */
class MessageMemoryManager {
    
    // 현재 메모리에 있는 메시지 목록 (timestamp 기준 내림차순 정렬)
    private val _messages = mutableListOf<ChatMessageUiModel>()
    val messages: List<ChatMessageUiModel> get() = _messages.toList()
    
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
        
        _messages.clear()
        _messages.addAll(messages.sortedByDescending { it.actualTimestamp })
        
        updateBoundaries()
        
        Log.d("MessageMemoryManager", "Initial messages set. Boundaries: oldest=${oldestMessageTimestamp}, newest=${newestMessageTimestamp}")
    }
    
    /**
     * 새로운 실시간 메시지 추가 (가장 최신 메시지로 추가)
     */
    fun addNewestMessage(message: ChatMessageUiModel): List<ChatMessageUiModel> {
        Log.d("MessageMemoryManager", "Adding newest message: ${message.chatId}")
        
        // 중복 방지
        if (_messages.any { it.chatId == message.chatId }) {
            Log.d("MessageMemoryManager", "Message already exists, skipping")
            return messages
        }
        
        // 최신 메시지로 추가 (리스트 맨 앞에)
        _messages.add(0, message)
        
        // 메모리 정리 수행
        performMemoryCleanupIfNeeded(CleanupDirection.REMOVE_OLDEST)
        
        updateBoundaries()
        
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
        
        // 중복 제거 및 정렬
        val newMessages = olderMessages
            .filter { newMsg -> _messages.none { it.chatId == newMsg.chatId } }
            .sortedByDescending { it.actualTimestamp }
        
        // 기존 메시지 뒤에 추가
        _messages.addAll(newMessages)
        _messages.sortByDescending { it.actualTimestamp }
        
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
        
        // 중복 제거 및 정렬
        val newMessages = newerMessages
            .filter { newMsg -> _messages.none { it.chatId == newMsg.chatId } }
            .sortedByDescending { it.actualTimestamp }
        
        // 기존 메시지 앞에 추가
        _messages.addAll(0, newMessages)
        
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
     */
    fun updateMessage(updater: (ChatMessageUiModel) -> ChatMessageUiModel?): List<ChatMessageUiModel> {
        for (i in _messages.indices) {
            val updated = updater(_messages[i])
            if (updated != null) {
                _messages[i] = updated
                Log.d("MessageMemoryManager", "Message updated: ${updated.chatId}")
                break
            }
        }
        return messages
    }
    
    /**
     * 특정 메시지 제거
     */
    fun removeMessage(messageId: String): List<ChatMessageUiModel> {
        val removed = _messages.removeAll { it.chatId == messageId }
        if (removed) {
            Log.d("MessageMemoryManager", "Message removed: $messageId")
            updateBoundaries()
        }
        return messages
    }
    
    /**
     * 메모리 정리가 필요한지 확인하고 수행
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
                    repeat(messagesToRemove) {
                        if (_messages.size > ChatMemoryConfig.MIN_MESSAGES_IN_MEMORY) {
                            removedMessages.add(_messages.removeLastOrNull() ?: return@repeat)
                        }
                    }
                    Log.d("MessageMemoryManager", "Removed ${removedMessages.size} oldest messages")
                }
                CleanupDirection.REMOVE_NEWEST -> {
                    // 가장 새로운 메시지들 제거 (리스트 앞에서부터)
                    repeat(messagesToRemove) {
                        if (_messages.size > ChatMemoryConfig.MIN_MESSAGES_IN_MEMORY) {
                            removedMessages.add(_messages.removeFirstOrNull() ?: return@repeat)
                        }
                    }
                    Log.d("MessageMemoryManager", "Removed ${removedMessages.size} newest messages")
                }
            }
        }
        
        return removedMessages
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