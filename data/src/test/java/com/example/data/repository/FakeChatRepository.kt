package com.example.data.repository

import com.example.domain.model.ChatMessage
import com.example.domain.model.MediaImage
import com.example.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * ChatRepository의 가짜(Fake) 구현체
 * 
 * 이 클래스는 테스트 용도로 ChatRepository 인터페이스를 인메모리 방식으로 구현합니다.
 * Firebase 의존성 없이 Repository 기능을 테스트할 수 있습니다.
 */
class FakeChatRepository : ChatRepository {
    
    // 채널별 인메모리 메시지 데이터 저장소
    private val channelMessages = ConcurrentHashMap<String, MutableList<ChatMessage>>()
    
    // 채널별 메시지 Flow
    private val channelMessageFlows = ConcurrentHashMap<String, MutableStateFlow<List<ChatMessage>>>()
    
    // 로컬 갤러리 이미지
    private val galleryImages = mutableListOf<MediaImage>()
    
    // 메시지 ID 생성기
    private val messageIdGenerator = AtomicInteger(1)
    
    // 에러 시뮬레이션을 위한 설정
    private var shouldSimulateError = false
    private var errorToSimulate: Exception = Exception("Simulated error")
    
    /**
     * 테스트를 위해 채널에 메시지 추가
     */
    fun addMessage(channelId: String, message: ChatMessage) {
        // 채널 메시지 목록 가져오기 또는 새로 생성
        val messages = channelMessages.getOrPut(channelId) { mutableListOf() }
        messages.add(message)
        
        // Flow 업데이트
        updateChannelFlow(channelId)
    }
    
    /**
     * 테스트를 위해 여러 메시지 한번에 추가
     */
    fun addMessages(channelId: String, messages: List<ChatMessage>) {
        // 채널 메시지 목록 가져오기 또는 새로 생성
        val existingMessages = channelMessages.getOrPut(channelId) { mutableListOf() }
        existingMessages.addAll(messages)
        
        // Flow 업데이트
        updateChannelFlow(channelId)
    }
    
    /**
     * 테스트를 위해 갤러리 이미지 설정
     */
    fun setGalleryImages(images: List<MediaImage>) {
        galleryImages.clear()
        galleryImages.addAll(images)
    }
    
    /**
     * 테스트를 위해 모든 메시지 초기화
     */
    fun clearMessages() {
        channelMessages.clear()
        channelMessageFlows.clear()
    }
    
    /**
     * 테스트를 위해 특정 채널의 메시지 초기화
     */
    fun clearChannelMessages(channelId: String) {
        channelMessages[channelId]?.clear()
        updateChannelFlow(channelId)
    }
    
    /**
     * 테스트를 위해 에러 시뮬레이션 설정
     */
    fun setShouldSimulateError(shouldError: Boolean, error: Exception = Exception("Simulated error")) {
        shouldSimulateError = shouldError
        errorToSimulate = error
    }
    
    /**
     * 채널 메시지 Flow 업데이트
     */
    private fun updateChannelFlow(channelId: String) {
        val messages = channelMessages[channelId] ?: mutableListOf()
        val flow = channelMessageFlows.getOrPut(channelId) { 
            MutableStateFlow(emptyList()) 
        }
        flow.value = messages.toList()
    }
    
    /**
     * 에러 시뮬레이션 확인 및 처리
     */
    private fun <T> simulateErrorIfNeeded(): Result<T>? {
        return if (shouldSimulateError) {
            Result.failure(errorToSimulate)
        } else {
            null
        }
    }
    
    override fun getMessagesStream(channelId: String): Flow<List<ChatMessage>> {
        // 채널별 Flow 반환 (없으면 빈 Flow 생성)
        return channelMessageFlows.getOrPut(channelId) { 
            MutableStateFlow(channelMessages[channelId]?.toList() ?: emptyList())
        }.asStateFlow()
    }
    
    override suspend fun fetchPastMessages(
        channelId: String, 
        beforeMessageId: Int, 
        limit: Int
    ): Result<List<ChatMessage>> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<List<ChatMessage>>()?.let { return it }
        
        // 채널 메시지 목록 가져오기
        val messages = channelMessages[channelId] ?: return Result.success(emptyList())
        
        // beforeMessageId 이전의 메시지들만 필터링
        val pastMessages = messages
            .filter { it.chatId < beforeMessageId }
            .sortedByDescending { it.chatId } // 최신순 정렬
            .take(limit)
        
        return Result.success(pastMessages)
    }
    
    override suspend fun sendMessage(
        channelId: String, 
        message: String, 
        attachmentUris: List<android.net.Uri>
    ): Result<ChatMessage> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<ChatMessage>()?.let { return it }
        
        // 유효성 검사
        if (message.isBlank() && attachmentUris.isEmpty()) {
            return Result.failure(IllegalArgumentException("Message is empty and no attachments provided"))
        }
        
        // 새 메시지 생성
        val newChatId = messageIdGenerator.getAndIncrement()
        val attachmentUrls = attachmentUris.map { it.toString() }
        
        val newMessage = ChatMessage(
            chatId = newChatId,
            channelId = channelId,
            userId = 1, // 테스트에서는 고정된 사용자 ID 사용
            userName = "Test User",
            userProfileUrl = null,
            message = message,
            sentAt = LocalDateTime.now(),
            isModified = false,
            attachmentImageUrls = attachmentUrls
        )
        
        // 메시지 저장 및 Flow 업데이트
        addMessage(channelId, newMessage)
        
        return Result.success(newMessage)
    }
    
    // 테스트용 오버로드 메서드 - 이름 변경으로 JVM 시그니처 충돌 해결
    suspend fun sendMessageTest(
        channelId: String, 
        message: String, 
        attachmentUris: List<TestUri>
    ): Result<ChatMessage> {
        // 유효성 검사
        if (message.isBlank() && attachmentUris.isEmpty()) {
            return Result.failure(IllegalArgumentException("Message is empty and no attachments provided"))
        }
        
        // 새 메시지 생성
        val newChatId = messageIdGenerator.getAndIncrement()
        val attachmentUrls = attachmentUris.map { it.toString() }
        
        val newMessage = ChatMessage(
            chatId = newChatId,
            channelId = channelId,
            userId = 1, // 테스트에서는 고정된 사용자 ID 사용
            userName = "Test User",
            userProfileUrl = null,
            message = message,
            sentAt = LocalDateTime.now(),
            isModified = false,
            attachmentImageUrls = attachmentUrls
        )
        
        // 메시지 저장 및 Flow 업데이트
        addMessage(channelId, newMessage)
        
        return Result.success(newMessage)
    }
    
    override suspend fun editMessage(
        channelId: String, 
        chatId: Int, 
        newMessage: String
    ): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 유효성 검사
        if (newMessage.isBlank()) {
            return Result.failure(IllegalArgumentException("New message cannot be blank"))
        }
        
        // 메시지 찾기
        val messages = channelMessages[channelId] ?: return Result.failure(
            NoSuchElementException("Channel not found: $channelId")
        )
        
        val messageIndex = messages.indexOfFirst { it.chatId == chatId }
        if (messageIndex == -1) {
            return Result.failure(NoSuchElementException("Message not found with ID: $chatId"))
        }
        
        // 메시지 업데이트
        val originalMessage = messages[messageIndex]
        val updatedMessage = originalMessage.copy(
            message = newMessage,
            isModified = true
        )
        
        messages[messageIndex] = updatedMessage
        updateChannelFlow(channelId)
        
        return Result.success(Unit)
    }
    
    override suspend fun deleteMessage(channelId: String, chatId: Int): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 메시지 찾기 및 삭제
        val messages = channelMessages[channelId] ?: return Result.failure(
            NoSuchElementException("Channel not found: $channelId")
        )
        
        val removed = messages.removeIf { it.chatId == chatId }
        if (!removed) {
            return Result.failure(NoSuchElementException("Message not found with ID: $chatId"))
        }
        
        // Flow 업데이트
        updateChannelFlow(channelId)
        
        return Result.success(Unit)
    }
    
    override suspend fun getLocalGalleryImages(page: Int, pageSize: Int): Result<List<MediaImage>> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<List<MediaImage>>()?.let { return it }
        
        // 페이징 적용
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, galleryImages.size)
        
        if (startIndex >= galleryImages.size) {
            return Result.success(emptyList()) // 더 이상 데이터 없음
        }
        
        return Result.success(galleryImages.subList(startIndex, endIndex))
    }
} 