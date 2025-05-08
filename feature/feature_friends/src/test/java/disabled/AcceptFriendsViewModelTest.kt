package com.example.feature_friends.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.data.repository.FakeFriendRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import com.example.domain.model.FriendRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * AcceptFriendsViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 AcceptFriendsViewModel의 기능을 검증합니다.
 * FakeFriendRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class AcceptFriendsViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: AcceptFriendsViewModel

    // Fake Repository
    private lateinit var fakeFriendRepository: FakeFriendRepository
    
    // SavedStateHandle Mock
    private lateinit var savedStateHandle: SavedStateHandle

    // 테스트 데이터
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
     * 초기 상태 테스트 - 빈 친구 요청 목록 및 로딩 상태
     */
    @Test
    fun `초기 상태는 빈 친구 요청 목록과 로딩 상태를 가져야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 빈 친구 요청 목록 상태의 repository

        // When: ViewModel 초기화
        viewModel = AcceptFriendsViewModel(savedStateHandle, fakeFriendRepository)
        
        // Then: 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertTrue(initialState.friendRequests.isEmpty())
        assertTrue(initialState.isLoading)
        assertNull(initialState.error)
    }

    /**
     * 친구 요청 목록 로딩 성공 테스트
     */
    @Test
    fun `Repository에서 친구 요청 목록을 성공적으로 가져와야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 친구 요청 데이터가 있는 repository
        fakeFriendRepository.addFriendRequest(testRequest1)
        fakeFriendRepository.addFriendRequest(testRequest2)
        
        // When: ViewModel 초기화 (이 때 init 블록에서 친구 요청 목록을 가져옴)
        viewModel = AcceptFriendsViewModel(savedStateHandle, fakeFriendRepository)
        
        // Then: UI 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(2, state.friendRequests.size)
        
        // 친구 요청 데이터 정확성 확인
        val request1 = state.friendRequests.find { it.userId == testRequest1.userId }
        assertNotNull(request1)
        assertEquals(testRequest1.userName, request1?.userName)
        assertEquals(testRequest1.profileImageUrl, request1?.profileImageUrl)
        
        // 로딩 상태 해제 확인
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    /**
     * 친구 요청 목록 가져오기 실패 테스트
     */
    @Test
    fun `Repository에서 오류 발생 시 에러 상태로 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러를 시뮬레이션하도록 설정
        fakeFriendRepository.setShouldSimulateError(true)
        
        // When: ViewModel 초기화 (init에서 loadFriendRequests 호출)
        viewModel = AcceptFriendsViewModel(savedStateHandle, fakeFriendRepository)
        
        // Then: 에러 상태 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.error)
        assertEquals("친구 요청 목록 로드 실패", state.error)
        assertFalse(state.isLoading)
        assertTrue(state.friendRequests.isEmpty())
    }

    /**
     * 친구 요청 수락 성공 테스트
     */
    @Test
    fun `친구 요청 수락 시 UI에서 요청이 즉시 제거되고 성공 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 친구 요청이 있는 상태
        fakeFriendRepository.addFriendRequest(testRequest1)
        fakeFriendRepository.addFriendRequest(testRequest2)
        viewModel = AcceptFriendsViewModel(savedStateHandle, fakeFriendRepository)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertEquals(2, initialState.friendRequests.size)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<AcceptFriendsEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 친구 요청 수락
        viewModel.acceptFriendRequest(testRequest1.userId)
        
        // Then: UI 업데이트 확인 (낙관적 업데이트로 요청이 즉시 제거됨)
        val updatedState = viewModel.uiState.getValue()
        assertEquals(1, updatedState.friendRequests.size)
        assertFalse(updatedState.friendRequests.any { it.userId == testRequest1.userId })
        
        // 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is AcceptFriendsEvent.ShowSnackbar)
        assertEquals("친구 요청을 수락했습니다.", (event as AcceptFriendsEvent.ShowSnackbar).message)
    }

    /**
     * 친구 요청 수락 실패 테스트
     */
    @Test
    fun `친구 요청 수락 실패 시 UI가 복구되고 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 친구 요청이 있고 에러를 시뮬레이션하도록 설정
        fakeFriendRepository.addFriendRequest(testRequest1)
        viewModel = AcceptFriendsViewModel(savedStateHandle, fakeFriendRepository)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertEquals(1, initialState.friendRequests.size)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<AcceptFriendsEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // 에러 설정
        fakeFriendRepository.setShouldSimulateError(true)
        
        // When: 친구 요청 수락 시도
        viewModel.acceptFriendRequest(testRequest1.userId)
        
        // Then: UI 복구 확인 (에러 발생으로 원래 상태로 돌아감)
        val updatedState = viewModel.uiState.getValue()
        assertEquals(1, updatedState.friendRequests.size) // 낙관적 업데이트 후 복구됨
        
        // 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is AcceptFriendsEvent.ShowSnackbar)
        assertTrue((event as AcceptFriendsEvent.ShowSnackbar).message.startsWith("요청 수락 실패"))
    }

    /**
     * 친구 요청 거절 성공 테스트
     */
    @Test
    fun `친구 요청 거절 시 UI에서 요청이 즉시 제거되고 성공 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 친구 요청이 있는 상태
        fakeFriendRepository.addFriendRequest(testRequest1)
        fakeFriendRepository.addFriendRequest(testRequest2)
        viewModel = AcceptFriendsViewModel(savedStateHandle, fakeFriendRepository)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertEquals(2, initialState.friendRequests.size)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<AcceptFriendsEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 친구 요청 거절
        viewModel.denyFriendRequest(testRequest1.userId)
        
        // Then: UI 업데이트 확인 (낙관적 업데이트로 요청이 즉시 제거됨)
        val updatedState = viewModel.uiState.getValue()
        assertEquals(1, updatedState.friendRequests.size)
        assertFalse(updatedState.friendRequests.any { it.userId == testRequest1.userId })
        
        // 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is AcceptFriendsEvent.ShowSnackbar)
        assertEquals("친구 요청을 거절했습니다.", (event as AcceptFriendsEvent.ShowSnackbar).message)
    }

    /**
     * 친구 요청 거절 실패 테스트
     */
    @Test
    fun `친구 요청 거절 실패 시 UI가 복구되고 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 친구 요청이 있고 에러를 시뮬레이션하도록 설정
        fakeFriendRepository.addFriendRequest(testRequest1)
        viewModel = AcceptFriendsViewModel(savedStateHandle, fakeFriendRepository)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertEquals(1, initialState.friendRequests.size)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<AcceptFriendsEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // 에러 설정
        fakeFriendRepository.setShouldSimulateError(true)
        
        // When: 친구 요청 거절 시도
        viewModel.denyFriendRequest(testRequest1.userId)
        
        // Then: UI 복구 확인 (에러 발생으로 원래 상태로 돌아감)
        val updatedState = viewModel.uiState.getValue()
        assertEquals(1, updatedState.friendRequests.size) // 낙관적 업데이트 후 복구됨
        
        // 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is AcceptFriendsEvent.ShowSnackbar)
        assertTrue((event as AcceptFriendsEvent.ShowSnackbar).message.startsWith("요청 거절 실패"))
    }

    /**
     * 존재하지 않는 친구 요청 처리 테스트
     */
    @Test
    fun `존재하지 않는 친구 요청 ID로 수락 시도 시 적절히 처리되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 친구 요청이 없는 상태
        viewModel = AcceptFriendsViewModel(savedStateHandle, fakeFriendRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<AcceptFriendsEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 존재하지 않는 ID로 수락 시도
        viewModel.acceptFriendRequest("nonexistent_id")
        
        // Then: 오류 메시지 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is AcceptFriendsEvent.ShowSnackbar)
        assertTrue((event as AcceptFriendsEvent.ShowSnackbar).message.contains("not found"))
    }
} 