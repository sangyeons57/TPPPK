package com.example.feature_friends.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.data.repository.FakeFriendRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import com.example.domain.model.Friend
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * FriendViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 FriendViewModel의 기능을 검증합니다.
 * FakeFriendRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class FriendViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: FriendViewModel

    // Fake Repository
    private lateinit var fakeFriendRepository: FakeFriendRepository
    
    // SavedStateHandle Mock
    private lateinit var savedStateHandle: SavedStateHandle

    // 테스트 데이터
    private val testFriend1 = Friend(
        userId = "friend_1",
        userName = "친구1",
        status = "온라인",
        profileImageUrl = "https://example.com/profile1.jpg"
    )
    
    private val testFriend2 = Friend(
        userId = "friend_2",
        userName = "친구2",
        status = "오프라인",
        profileImageUrl = null
    )

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // SavedStateHandle Mock 설정
        savedStateHandle = mock(SavedStateHandle::class.java)
        
        // Fake Repository 초기화
        fakeFriendRepository = FakeFriendRepository()
    }

    /**
     * 초기 상태 테스트 - 빈 친구 목록 및 로딩 상태
     */
    @Test
    fun `초기 상태는 빈 친구 목록과 로딩 상태를 가져야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 빈 친구 목록 상태의 repository

        // When: ViewModel 초기화
        viewModel = FriendViewModel(savedStateHandle, fakeFriendRepository)
        
        // Then: 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertTrue(initialState.friends.isEmpty())
        assertTrue(initialState.isLoading)
        assertNull(initialState.error)
        assertFalse(initialState.showAddFriendDialog)
    }

    /**
     * 친구 목록 로딩 성공 테스트
     */
    @Test
    fun `Repository에서 친구 목록을 성공적으로 가져와야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 친구 데이터가 있는 repository
        fakeFriendRepository.addFriend(testFriend1)
        fakeFriendRepository.addFriend(testFriend2)
        
        // When: ViewModel 초기화 (이 때 init 블록에서 친구 목록을 가져옴)
        viewModel = FriendViewModel(savedStateHandle, fakeFriendRepository)
        
        // Then: UI 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(2, state.friends.size)
        
        // 친구 데이터 정확성 확인
        val friend1 = state.friends.find { it.userId == testFriend1.userId }
        assertNotNull(friend1)
        assertEquals(testFriend1.userName, friend1?.userName)
        assertEquals(testFriend1.status, friend1?.status)
        assertEquals(testFriend1.profileImageUrl, friend1?.profileImageUrl)
        
        // 로딩 상태 해제 확인
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    /**
     * 친구 목록 새로고침 테스트
     */
    @Test
    fun `refreshFriendsList는 친구 목록을 새로고침해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel과 친구 있는 상태
        fakeFriendRepository.addFriend(testFriend1)
        viewModel = FriendViewModel(savedStateHandle, fakeFriendRepository)
        
        // 초기 상태 확인
        assertEquals(1, viewModel.uiState.getValue().friends.size)
        
        // 새 친구 추가 (아직 UI에 반영 안 됨)
        fakeFriendRepository.addFriend(testFriend2)
        
        // When: 새로고침 요청
        viewModel.refreshFriendsList()
        
        // Then: 업데이트된 친구 목록 확인
        val state = viewModel.uiState.getValue()
        assertEquals(2, state.friends.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    /**
     * 친구 목록 가져오기 실패 테스트
     */
    @Test
    fun `Repository에서 오류 발생 시 에러 상태로 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러를 시뮬레이션하도록 설정
        fakeFriendRepository.setShouldSimulateError(true)
        
        // When: 새로고침 시도
        viewModel = FriendViewModel(savedStateHandle, fakeFriendRepository)
        viewModel.refreshFriendsList()
        
        // Then: 에러 상태 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.error)
        assertEquals("친구 목록 새로고침 실패", state.error)
        assertFalse(state.isLoading)
        
        // 이벤트 확인
        val eventCollector = EventCollector<FriendsEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is FriendsEvent.ShowSnackbar)
        assertEquals("새로고침 실패", (event as FriendsEvent.ShowSnackbar).message)
    }

    /**
     * 친구 클릭 시 채팅 화면으로 이동 테스트
     */
    @Test
    fun `친구 클릭 시 해당 채팅방으로 이동 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 친구와 DM 채널이 설정된 상태
        fakeFriendRepository.addFriend(testFriend1)
        fakeFriendRepository.setDmChannelId(testFriend1.userId, "dm_channel_1")
        viewModel = FriendViewModel(savedStateHandle, fakeFriendRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<FriendsEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 친구 클릭
        viewModel.onFriendClick(testFriend1.userId)
        
        // Then: 채팅방으로 이동 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is FriendsEvent.NavigateToChat)
        assertEquals("dm_channel_1", (event as FriendsEvent.NavigateToChat).channelId)
    }

    /**
     * 친구 클릭 시 채팅방 정보 가져오기 실패 테스트
     */
    @Test
    fun `친구 클릭 시 채팅방 정보 가져오기 실패하면 스낵바 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 친구는 있지만 DM 채널 정보가 없는 상태
        fakeFriendRepository.addFriend(testFriend1)
        viewModel = FriendViewModel(savedStateHandle, fakeFriendRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<FriendsEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 친구 클릭
        viewModel.onFriendClick(testFriend1.userId)
        
        // Then: 오류 메시지 스낵바 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is FriendsEvent.ShowSnackbar)
        assertEquals("채팅방 정보를 가져올 수 없습니다.", (event as FriendsEvent.ShowSnackbar).message)
    }

    /**
     * 친구 요청 수락 화면으로 이동 테스트
     */
    @Test
    fun `친구 요청 수락 버튼 클릭 시 친구 요청 화면으로 이동 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = FriendViewModel(savedStateHandle, fakeFriendRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<FriendsEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 친구 요청 수락 버튼 클릭
        viewModel.onAcceptFriendClick()
        
        // Then: 친구 요청 화면으로 이동 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is FriendsEvent.NavigateToAcceptFriends)
    }
} 