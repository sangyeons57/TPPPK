package com.example.data.repository

import com.example.domain.model.Friend
import com.example.domain.model.FriendRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.NoSuchElementException

/**
 * FriendRepository 기능 테스트
 *
 * 이 테스트는 FakeFriendRepository를 사용하여 FriendRepository 인터페이스의
 * 모든 기능이 예상대로 동작하는지 검증합니다.
 */
class FriendRepositoryTest {

    // 테스트 대상 (SUT: System Under Test)
    private lateinit var friendRepository: FakeFriendRepository
    
    // 테스트 데이터
    private val testFriend1 = Friend(
        userId = "user_1",
        userName = "사용자1",
        status = "온라인",
        profileImageUrl = "https://example.com/profile1.jpg"
    )
    
    private val testFriend2 = Friend(
        userId = "user_2",
        userName = "사용자2",
        status = "오프라인",
        profileImageUrl = null
    )
    
    private val testRequest1 = FriendRequest(
        userId = "req_user_1",
        userName = "요청자1",
        profileImageUrl = "https://example.com/req1.jpg"
    )
    
    private val testRequest2 = FriendRequest(
        userId = "req_user_2",
        userName = "요청자2",
        profileImageUrl = null
    )
    
    /**
     * 각 테스트 전 설정
     */
    @Before
    fun setup() {
        // FakeFriendRepository 초기화
        friendRepository = FakeFriendRepository()
        
        // 테스트 친구 추가
        friendRepository.addFriend(testFriend1)
        
        // 테스트 친구 요청 추가
        friendRepository.addFriendRequest(testRequest1)
        
        // 테스트 DM 채널 설정
        friendRepository.setDmChannelId(testFriend1.userId, "dm_channel_1")
    }
    
    /**
     * 친구 목록 스트림 테스트
     */
    @Test
    fun `getFriendsListStream should emit friends`() = runBlocking {
        // When: 친구 목록 스트림 가져오기
        val friends = friendRepository.getFriendsListStream().first()
        
        // Then: 올바른 친구 목록 확인
        assertEquals(1, friends.size)
        assertEquals(testFriend1, friends.first())
    }
    
    /**
     * 친구 목록 가져오기 테스트
     */
    @Test
    fun `fetchFriendsList should update the stream`() = runBlocking {
        // Given: 초기 상태 확인
        val initialFriends = friendRepository.getFriendsListStream().first()
        assertEquals(1, initialFriends.size)
        
        // 새 친구 추가 (직접 추가하지만 Stream에는 아직 반영되지 않았다고 가정)
        friendRepository.addFriend(testFriend2)
        
        // When: 친구 목록 가져오기
        val result = friendRepository.fetchFriendsList()
        
        // Then: 성공 및 업데이트된 목록 확인
        assertTrue(result.isSuccess)
        
        // 스트림에서 업데이트된 목록 확인
        val updatedFriends = friendRepository.getFriendsListStream().first()
        assertEquals(2, updatedFriends.size)
    }
    
    /**
     * DM 채널 ID 가져오기 테스트
     */
    @Test
    fun `getDmChannelId should return channel id for a friend`() = runBlocking {
        // When: 친구의 DM 채널 ID 가져오기
        val result = friendRepository.getDmChannelId(testFriend1.userId)
        
        // Then: 성공 및 올바른 채널 ID 확인
        assertTrue(result.isSuccess)
        assertEquals("dm_channel_1", result.getOrNull())
    }
    
    /**
     * 존재하지 않는 친구의 DM 채널 ID 가져오기 실패 테스트
     */
    @Test
    fun `getDmChannelId should fail for non-existent friend`() = runBlocking {
        // When: 존재하지 않는 친구의 DM 채널 ID 가져오기
        val result = friendRepository.getDmChannelId("nonexistent_user")
        
        // Then: 실패 및 적절한 예외 확인
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is NoSuchElementException)
        assertTrue(exception?.message?.contains("Friend not found") == true)
    }
    
    /**
     * DM 채널 ID가 설정되지 않은 친구의 채널 ID 가져오기 실패 테스트
     */
    @Test
    fun `getDmChannelId should fail for friend with no DM channel`() = runBlocking {
        // Given: 채널 ID가 없는 친구 추가
        friendRepository.addFriend(testFriend2)
        
        // When: 채널 ID가 없는 친구의 DM 채널 ID 가져오기
        val result = friendRepository.getDmChannelId(testFriend2.userId)
        
        // Then: 실패 및 적절한 예외 확인
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is NoSuchElementException)
        assertTrue(exception?.message?.contains("DM channel not found") == true)
    }
    
    /**
     * 친구 요청 보내기 테스트
     */
    @Test
    fun `sendFriendRequest should return success message`() = runBlocking {
        // When: 친구 요청 보내기
        val result = friendRepository.sendFriendRequest("새로운사용자")
        
        // Then: 성공 및 메시지 확인
        assertTrue(result.isSuccess)
        val message = result.getOrNull()
        assertNotNull(message)
        assertTrue(message?.contains("친구 요청") == true)
    }
    
    /**
     * 빈 사용자 이름으로 친구 요청 보내기 실패 테스트
     */
    @Test
    fun `sendFriendRequest should fail with empty username`() = runBlocking {
        // When: 빈 사용자 이름으로 친구 요청 보내기
        val result = friendRepository.sendFriendRequest("")
        
        // Then: 실패 및 적절한 예외 확인
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
    }
    
    /**
     * 이미 친구인 사용자에게 친구 요청 보내기 실패 테스트
     */
    @Test
    fun `sendFriendRequest should fail if user is already a friend`() = runBlocking {
        // When: 이미 친구인 사용자에게 친구 요청 보내기
        val result = friendRepository.sendFriendRequest(testFriend1.userName)
        
        // Then: 실패 및 적절한 예외 확인
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertTrue(exception?.message?.contains("already a friend") == true)
    }
    
    /**
     * 친구 요청 목록 가져오기 테스트
     */
    @Test
    fun `getFriendRequests should return all requests`() = runBlocking {
        // Given: 추가 요청 추가
        friendRepository.addFriendRequest(testRequest2)
        
        // When: 요청 목록 가져오기
        val result = friendRepository.getFriendRequests()
        
        // Then: 성공 및 요청 목록 확인
        assertTrue(result.isSuccess)
        val requests = result.getOrNull()
        assertNotNull(requests)
        assertEquals(2, requests?.size)
        assertTrue(requests?.contains(testRequest1) == true)
        assertTrue(requests?.contains(testRequest2) == true)
    }
    
    /**
     * 친구 요청 수락 테스트
     */
    @Test
    fun `acceptFriendRequest should add friend and remove request`() = runBlocking {
        // Given: 초기 상태 확인
        val initialFriends = friendRepository.getFriendsListStream().first()
        assertEquals(1, initialFriends.size)
        
        // When: 친구 요청 수락
        val result = friendRepository.acceptFriendRequest(testRequest1.userId)
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // 친구 목록에 추가되었는지 확인
        val updatedFriends = friendRepository.getFriendsListStream().first()
        assertEquals(2, updatedFriends.size)
        
        // 새 친구 정보 확인
        val newFriend = updatedFriends.find { it.userId == testRequest1.userId }
        assertNotNull(newFriend)
        assertEquals(testRequest1.userName, newFriend?.userName)
        assertEquals(testRequest1.profileImageUrl, newFriend?.profileImageUrl)
        
        // 요청이 제거되었는지 확인
        val requests = friendRepository.getFriendRequests().getOrNull()
        assertEquals(0, requests?.size)
    }
    
    /**
     * 존재하지 않는 요청 수락 실패 테스트
     */
    @Test
    fun `acceptFriendRequest should fail for non-existent request`() = runBlocking {
        // When: 존재하지 않는 요청 수락 시도
        val result = friendRepository.acceptFriendRequest("nonexistent_request")
        
        // Then: 실패 및 적절한 예외 확인
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is NoSuchElementException)
    }
    
    /**
     * 친구 요청 거절 테스트
     */
    @Test
    fun `denyFriendRequest should remove request`() = runBlocking {
        // Given: 초기 상태 확인
        val initialRequests = friendRepository.getFriendRequests().getOrNull()
        assertEquals(1, initialRequests?.size)
        
        // When: 친구 요청 거절
        val result = friendRepository.denyFriendRequest(testRequest1.userId)
        
        // Then: 성공
        assertTrue(result.isSuccess)
        
        // 요청이 제거되었는지 확인
        val updatedRequests = friendRepository.getFriendRequests().getOrNull()
        assertEquals(0, updatedRequests?.size)
        
        // 친구 목록은 변경되지 않았는지 확인
        val friends = friendRepository.getFriendsListStream().first()
        assertEquals(1, friends.size)
    }
    
    /**
     * 존재하지 않는 요청 거절 실패 테스트
     */
    @Test
    fun `denyFriendRequest should fail for non-existent request`() = runBlocking {
        // When: 존재하지 않는 요청 거절 시도
        val result = friendRepository.denyFriendRequest("nonexistent_request")
        
        // Then: 실패 및 적절한 예외 확인
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is NoSuchElementException)
    }
    
    /**
     * 에러 시뮬레이션 테스트
     */
    @Test
    fun `repository should propagate simulated errors`() = runBlocking {
        // Given: 에러 시뮬레이션 설정
        val testError = IllegalStateException("Test error")
        friendRepository.setShouldSimulateError(true, testError)
        
        // When: 다양한 작업 시도
        val fetchResult = friendRepository.fetchFriendsList()
        val dmChannelResult = friendRepository.getDmChannelId(testFriend1.userId)
        val sendRequestResult = friendRepository.sendFriendRequest("새사용자")
        val getRequestsResult = friendRepository.getFriendRequests()
        
        // Then: 모든 결과가 동일한 에러를 반환해야 함
        assertTrue(fetchResult.isFailure)
        assertEquals(testError, fetchResult.exceptionOrNull())
        
        assertTrue(dmChannelResult.isFailure)
        assertEquals(testError, dmChannelResult.exceptionOrNull())
        
        assertTrue(sendRequestResult.isFailure)
        assertEquals(testError, sendRequestResult.exceptionOrNull())
        
        assertTrue(getRequestsResult.isFailure)
        assertEquals(testError, getRequestsResult.exceptionOrNull())
    }
    
    /**
     * 친구 추가/제거 헬퍼 메서드 테스트
     */
    @Test
    fun `helper methods should correctly manipulate friends`() = runBlocking {
        // Given: 초기 상태 확인
        val initialFriends = friendRepository.getFriendsListStream().first()
        assertEquals(1, initialFriends.size)
        
        // When: 친구 추가
        friendRepository.addFriend(testFriend2)
        
        // Then: 친구 리스트 업데이트 확인
        val friendsAfterAdd = friendRepository.getFriendsListStream().first()
        assertEquals(2, friendsAfterAdd.size)
        
        // When: 친구 제거
        friendRepository.removeFriend(testFriend1.userId)
        
        // Then: 친구 제거 확인
        val friendsAfterRemove = friendRepository.getFriendsListStream().first()
        assertEquals(1, friendsAfterRemove.size)
        assertEquals(testFriend2, friendsAfterRemove.first())
        
        // When: 여러 친구 추가
        friendRepository.addFriends(listOf(testFriend1))
        
        // Then: 여러 친구 추가 확인
        val friendsAfterBulkAdd = friendRepository.getFriendsListStream().first()
        assertEquals(2, friendsAfterBulkAdd.size)
        
        // When: 모든 친구 제거
        friendRepository.clearFriends()
        
        // Then: 모든 친구 제거 확인
        val friendsAfterClear = friendRepository.getFriendsListStream().first()
        assertTrue(friendsAfterClear.isEmpty())
    }
    
    /**
     * 친구 요청 추가/제거 헬퍼 메서드 테스트
     */
    @Test
    fun `helper methods should correctly manipulate friend requests`() = runBlocking {
        // Given: 초기 상태 확인
        val initialRequests = friendRepository.getFriendRequests().getOrNull()
        assertEquals(1, initialRequests?.size)
        
        // When: 요청 추가
        friendRepository.addFriendRequest(testRequest2)
        
        // Then: 요청 추가 확인
        val requestsAfterAdd = friendRepository.getFriendRequests().getOrNull()
        assertEquals(2, requestsAfterAdd?.size)
        
        // When: 요청 제거
        friendRepository.removeFriendRequest(testRequest1.userId)
        
        // Then: 요청 제거 확인
        val requestsAfterRemove = friendRepository.getFriendRequests().getOrNull()
        assertEquals(1, requestsAfterRemove?.size)
        assertEquals(testRequest2, requestsAfterRemove?.first())
        
        // When: 모든 요청 제거
        friendRepository.clearFriendRequests()
        
        // Then: 모든 요청 제거 확인
        val requestsAfterClear = friendRepository.getFriendRequests().getOrNull()
        assertTrue(requestsAfterClear?.isEmpty() == true)
    }
} 