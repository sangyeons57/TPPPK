package com.example.data.repository

import com.example.domain.model.DmConversation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * DmRepository 기능 테스트
 *
 * 이 테스트는 FakeDmRepository를 사용하여 DmRepository 인터페이스의
 * 모든 기능이 예상대로 동작하는지 검증합니다.
 */
class DmRepositoryTest {

    // 테스트 대상 (SUT: System Under Test)
    private lateinit var dmRepository: FakeDmRepository
    
    // 테스트 데이터
    private val testChannelId = "dm_test_123"
    private val testPartnerId = "partner_456"
    private val testPartnerName = "테스트 파트너"
    private val testConversation = DmConversation(
        channelId = testChannelId,
        partnerUserId = testPartnerId,
        partnerUserName = testPartnerName,
        partnerProfileImageUrl = null,
        lastMessage = "안녕하세요",
        lastMessageTimestamp = LocalDateTime.now().minusHours(1),
        unreadCount = 2
    )
    
    // 추가 테스트 대화
    private val additionalConversations = listOf(
        DmConversation(
            channelId = "dm_test_234",
            partnerUserId = "partner_789",
            partnerUserName = "다른 파트너",
            partnerProfileImageUrl = "https://example.com/profile.jpg",
            lastMessage = "프로젝트 진행 상황이 어떻게 되나요?",
            lastMessageTimestamp = LocalDateTime.now().minusMinutes(30),
            unreadCount = 1
        ),
        DmConversation(
            channelId = "dm_test_345",
            partnerUserId = "partner_012",
            partnerUserName = "또 다른 파트너",
            partnerProfileImageUrl = null,
            lastMessage = "미팅 시간을 조정해 주세요",
            lastMessageTimestamp = LocalDateTime.now().minusMinutes(5),
            unreadCount = 0
        )
    )
    
    /**
     * 각 테스트 전 설정
     */
    @Before
    fun setup() {
        // FakeDmRepository 초기화
        dmRepository = FakeDmRepository()
        
        // 테스트 대화 추가
        dmRepository.addDmConversation(testConversation)
    }
    
    /**
     * DM 목록 스트림 테스트
     */
    @Test
    fun `getDmListStream should emit conversations`() = runBlocking {
        // When: DM 목록 스트림 가져오기
        val conversations = dmRepository.getDmListStream().first()
        
        // Then: 올바른 대화 목록 확인
        assertEquals(1, conversations.size)
        assertEquals(testConversation, conversations.first())
    }
    
    /**
     * 여러 DM 대화가 있을 때 최신 메시지 순 정렬 테스트
     */
    @Test
    fun `getDmListStream should return conversations sorted by timestamp`() = runBlocking {
        // Given: 추가 대화 설정
        dmRepository.addDmConversations(additionalConversations)
        
        // When: DM 목록 스트림 가져오기
        val conversations = dmRepository.getDmListStream().first()
        
        // Then: 최신 메시지 순으로 정렬되어 있는지 확인
        assertEquals(3, conversations.size)
        
        // 최신 메시지가 먼저 오는지 확인
        for (i in 0 until conversations.size - 1) {
            val current = conversations[i].lastMessageTimestamp
            val next = conversations[i + 1].lastMessageTimestamp
            
            if (current != null && next != null) {
                assertTrue(current.isAfter(next) || current.isEqual(next))
            }
        }
    }
    
    /**
     * DM 목록 가져오기 테스트
     */
    @Test
    fun `fetchDmList should update the stream`() = runBlocking {
        // Given: 초기 상태 확인
        val initialConversations = dmRepository.getDmListStream().first()
        assertEquals(1, initialConversations.size)
        
        // 새 대화 추가 (직접 추가하지만 Stream에는 아직 반영되지 않았다고 가정)
        dmRepository.addDmConversations(additionalConversations)
        
        // When: DM 목록 가져오기
        val result = dmRepository.fetchDmList()
        
        // Then: 성공 및 업데이트된 목록 확인
        assertTrue(result.isSuccess)
        
        // 스트림에서 업데이트된 목록 확인
        val updatedConversations = dmRepository.getDmListStream().first()
        assertEquals(3, updatedConversations.size)
    }
    
    /**
     * 새 DM 대화 생성 테스트
     */
    @Test
    fun `createDmConversation should add new conversation`() = runBlocking {
        // Given: 새 파트너 정보
        val newPartnerId = "new_partner_123"
        val newPartnerName = "새 파트너"
        
        // When: 새 대화 생성
        val result = dmRepository.createDmConversation(
            partnerUserId = newPartnerId,
            partnerUserName = newPartnerName
        )
        
        // Then: 성공 및 생성된 대화 확인
        assertTrue(result.isSuccess)
        val newConversation = result.getOrNull()
        assertNotNull(newConversation)
        assertEquals(newPartnerId, newConversation?.partnerUserId)
        assertEquals(newPartnerName, newConversation?.partnerUserName)
        
        // 스트림에 새 대화가 추가되었는지 확인
        val updatedConversations = dmRepository.getDmListStream().first()
        assertEquals(2, updatedConversations.size)
    }
    
    /**
     * 메시지 추가 테스트
     */
    @Test
    fun `addMessage should update last message and timestamp`() = runBlocking {
        // Given: 추가할 메시지
        val newMessage = "새로운 메시지입니다"
        
        // When: 메시지 추가
        val result = dmRepository.addMessage(
            channelId = testChannelId,
            message = newMessage
        )
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // 대화가 업데이트되었는지 확인
        val conversations = dmRepository.getDmListStream().first()
        val updatedConversation = conversations.find { it.channelId == testChannelId }
        
        assertNotNull(updatedConversation)
        assertEquals(newMessage, updatedConversation?.lastMessage)
        assertNotNull(updatedConversation?.lastMessageTimestamp)
        
        // 읽지 않은 메시지 수가 증가했는지 확인
        assertEquals(testConversation.unreadCount + 1, updatedConversation?.unreadCount)
    }
    
    /**
     * 현재 사용자가 보낸 메시지 추가 테스트
     */
    @Test
    fun `addMessage should not increase unread count if sent by current user`() = runBlocking {
        // Given: 현재 사용자가 보낼 메시지
        val newMessage = "내가 보낸 메시지입니다"
        
        // When: 메시지 추가 (fromCurrentUser = true)
        val result = dmRepository.addMessage(
            channelId = testChannelId,
            message = newMessage,
            fromCurrentUser = true
        )
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // 대화가 업데이트되었는지 확인
        val conversations = dmRepository.getDmListStream().first()
        val updatedConversation = conversations.find { it.channelId == testChannelId }
        
        assertNotNull(updatedConversation)
        assertEquals(newMessage, updatedConversation?.lastMessage)
        
        // 읽지 않은 메시지 수가 변하지 않아야 함
        assertEquals(0, updatedConversation?.unreadCount)
    }
    
    /**
     * 존재하지 않는 채널에 메시지 추가 실패 테스트
     */
    @Test
    fun `addMessage should fail for non-existent channel`() = runBlocking {
        // When: 존재하지 않는 채널에 메시지 추가 시도
        val result = dmRepository.addMessage(
            channelId = "nonexistent_channel",
            message = "테스트 메시지"
        )
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is NoSuchElementException)
    }
    
    /**
     * 읽음 처리 테스트
     */
    @Test
    fun `markAsRead should set unread count to zero`() = runBlocking {
        // Given: 읽지 않은
        // When: 읽음 처리
        val result = dmRepository.markAsRead(testChannelId)
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // 읽지 않은 메시지 수가 0이 되었는지 확인
        val conversations = dmRepository.getDmListStream().first()
        val updatedConversation = conversations.find { it.channelId == testChannelId }
        
        assertNotNull(updatedConversation)
        assertEquals(0, updatedConversation?.unreadCount)
    }
    
    /**
     * 존재하지 않는 채널 읽음 처리 실패 테스트
     */
    @Test
    fun `markAsRead should fail for non-existent channel`() = runBlocking {
        // When: 존재하지 않는 채널 읽음 처리 시도
        val result = dmRepository.markAsRead("nonexistent_channel")
        
        // Then: 실패 및 적절한 에러 메시지
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is NoSuchElementException)
    }
    
    /**
     * 에러 시뮬레이션 테스트
     */
    @Test
    fun `repository should propagate simulated errors`() = runBlocking {
        // Given: 에러 시뮬레이션 설정
        val testError = IllegalStateException("Test error")
        dmRepository.setShouldSimulateError(true, testError)
        
        // When: 다양한 작업 시도
        val fetchResult = dmRepository.fetchDmList()
        val createResult = dmRepository.createDmConversation("user_id", "User Name")
        val messageResult = dmRepository.addMessage(testChannelId, "메시지")
        
        // Then: 모든 결과가 동일한 에러를 반환해야 함
        assertTrue(fetchResult.isFailure)
        assertEquals(testError, fetchResult.exceptionOrNull())
        
        assertTrue(createResult.isFailure)
        assertEquals(testError, createResult.exceptionOrNull())
        
        assertTrue(messageResult.isFailure)
        assertEquals(testError, messageResult.exceptionOrNull())
    }
    
    /**
     * DM 대화 제거 테스트
     */
    @Test
    fun `removeDmConversation should remove conversation from list`() = runBlocking {
        // Given: 초기 상태 확인
        val initialConversations = dmRepository.getDmListStream().first()
        assertEquals(1, initialConversations.size)
        
        // When: 대화 제거
        dmRepository.removeDmConversation(testChannelId)
        
        // Then: 목록이 비어 있는지 확인
        val updatedConversations = dmRepository.getDmListStream().first()
        assertTrue(updatedConversations.isEmpty())
    }
    
    /**
     * 모든 DM 대화 초기화 테스트
     */
    @Test
    fun `clearDmConversations should remove all conversations`() = runBlocking {
        // Given: 여러 대화 추가
        dmRepository.addDmConversations(additionalConversations)
        val initialConversations = dmRepository.getDmListStream().first()
        assertEquals(3, initialConversations.size)
        
        // When: 모든 대화 초기화
        dmRepository.clearDmConversations()
        
        // Then: 목록이 비어 있는지 확인
        val updatedConversations = dmRepository.getDmListStream().first()
        assertTrue(updatedConversations.isEmpty())
    }
} 