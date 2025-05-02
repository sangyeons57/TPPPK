package com.example.data.repository

import com.example.domain.model.DmConversation
import com.example.domain.repository.DmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * DmRepository의 가짜(Fake) 구현체
 * 
 * 이 클래스는 테스트 용도로 DmRepository 인터페이스를 인메모리 방식으로 구현합니다.
 * Firebase 의존성 없이 Repository 기능을 테스트할 수 있습니다.
 */
class FakeDmRepository : DmRepository {
    
    // 인메모리 DM 대화 데이터 저장소
    private val dmConversations = ConcurrentHashMap<String, DmConversation>()
    
    // DM 목록 Flow
    private val dmListFlow = MutableStateFlow<List<DmConversation>>(emptyList())
    
    // 에러 시뮬레이션을 위한 설정
    private var shouldSimulateError = false
    private var errorToSimulate: Exception = Exception("Simulated error")
    
    /**
     * 테스트를 위해 DM 대화 추가
     */
    fun addDmConversation(conversation: DmConversation) {
        dmConversations[conversation.channelId] = conversation
        updateDmListFlow()
    }
    
    /**
     * 테스트를 위해 DM 대화 추가 (여러 개)
     */
    fun addDmConversations(conversations: List<DmConversation>) {
        conversations.forEach { conversation ->
            dmConversations[conversation.channelId] = conversation
        }
        updateDmListFlow()
    }
    
    /**
     * 테스트를 위해 특정 DM 대화 삭제
     */
    fun removeDmConversation(channelId: String) {
        dmConversations.remove(channelId)
        updateDmListFlow()
    }
    
    /**
     * 테스트를 위해 모든 DM 대화 초기화
     */
    fun clearDmConversations() {
        dmConversations.clear()
        updateDmListFlow()
    }
    
    /**
     * 테스트를 위해 에러 시뮬레이션 설정
     */
    fun setShouldSimulateError(shouldError: Boolean, error: Exception = Exception("Simulated error")) {
        shouldSimulateError = shouldError
        errorToSimulate = error
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
    
    /**
     * DM 목록 Flow 업데이트
     */
    private fun updateDmListFlow() {
        // 최신 메시지 기준으로 정렬
        val sortedList = dmConversations.values
            .sortedByDescending { it.lastMessageTimestamp }
            .toList()
        
        dmListFlow.value = sortedList
    }
    
    override fun getDmListStream(): Flow<List<DmConversation>> {
        return dmListFlow.asStateFlow()
    }
    
    override suspend fun fetchDmList(): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 실제 구현에서는 서버에서 데이터를 가져옴
        // 테스트에서는 이미 설정된 인메모리 데이터를 그대로 사용
        updateDmListFlow()
        
        return Result.success(Unit)
    }
    
    /**
     * 새 DM 대화 생성 (테스트 확장 메서드)
     */
    suspend fun createDmConversation(
        partnerUserId: String,
        partnerUserName: String,
        partnerProfileImageUrl: String? = null
    ): Result<DmConversation> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<DmConversation>()?.let { return it }
        
        // 채널 ID 생성 (실제로는 고유한 ID 생성 로직이 필요)
        val channelId = "dm_${partnerUserId}_${System.currentTimeMillis()}"
        
        // 새 DM 대화 생성
        val newConversation = DmConversation(
            channelId = channelId,
            partnerUserId = partnerUserId,
            partnerUserName = partnerUserName,
            partnerProfileImageUrl = partnerProfileImageUrl,
            lastMessage = null,
            lastMessageTimestamp = null,
            unreadCount = 0
        )
        
        // 대화 저장 및 Flow 업데이트
        dmConversations[channelId] = newConversation
        updateDmListFlow()
        
        return Result.success(newConversation)
    }
    
    /**
     * 새 메시지 추가 (테스트 확장 메서드)
     */
    suspend fun addMessage(
        channelId: String,
        message: String,
        fromCurrentUser: Boolean = false
    ): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 대화 찾기
        val conversation = dmConversations[channelId] ?: return Result.failure(
            NoSuchElementException("DM conversation not found: $channelId")
        )
        
        // 대화 업데이트
        val updatedConversation = conversation.copy(
            lastMessage = message,
            lastMessageTimestamp = LocalDateTime.now(),
            unreadCount = if (fromCurrentUser) 0 else conversation.unreadCount + 1
        )
        
        // 업데이트된 대화 저장 및 Flow 업데이트
        dmConversations[channelId] = updatedConversation
        updateDmListFlow()
        
        return Result.success(Unit)
    }
    
    /**
     * 읽음 처리 (테스트 확장 메서드)
     */
    suspend fun markAsRead(channelId: String): Result<Unit> {
        // 에러 시뮬레이션 확인
        simulateErrorIfNeeded<Unit>()?.let { return it }
        
        // 대화 찾기
        val conversation = dmConversations[channelId] ?: return Result.failure(
            NoSuchElementException("DM conversation not found: $channelId")
        )
        
        // 읽음 처리
        val updatedConversation = conversation.copy(unreadCount = 0)
        
        // 업데이트된 대화 저장 및 Flow 업데이트
        dmConversations[channelId] = updatedConversation
        updateDmListFlow()
        
        return Result.success(Unit)
    }
} 